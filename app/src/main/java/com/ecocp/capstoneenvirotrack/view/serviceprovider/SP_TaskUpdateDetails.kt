package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.R
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class SP_TaskUpdateDetails : Fragment() {

    private var bookingId: String? = null
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val uploadedFiles = mutableListOf<Uri>()

    private lateinit var txtStatusPill: TextView
    private lateinit var txtNoAttachments: TextView
    private lateinit var attachmentContainer: LinearLayout
    private lateinit var spinnerStatus: Spinner
    private lateinit var btnSaveStatus: Button
    private lateinit var btnCancel: Button
    private lateinit var btnUpload: Button

    private enum class BookingSource { TRANSPORT, TSD, UNKNOWN }
    private var bookingSource = BookingSource.UNKNOWN

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (!uris.isNullOrEmpty()) {
                uploadedFiles.addAll(uris)
                displayUploadedFiles()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookingId = arguments?.getString("bookingId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_sp_task_update_details, container, false)

        val txtCompanyName = view.findViewById<TextView>(R.id.txtCompanyName)
        val txtCompanyAddress = view.findViewById<TextView>(R.id.txtCompanyAddress)
        val txtTaskRef = view.findViewById<TextView>(R.id.txtTaskRef)
        txtStatusPill = view.findViewById(R.id.txtStatusPill)
        val txtOriginDestination = view.findViewById<TextView>(R.id.txtOriginDestination)
        val txtWasteType = view.findViewById<TextView>(R.id.txtWasteType)
        val txtQuantity = view.findViewById<TextView>(R.id.txtQuantity)
        val txtPackaging = view.findViewById<TextView>(R.id.txtPackaging)
        val txtSpecialInstructions = view.findViewById<TextView>(R.id.txtSpecialInstructions)

        attachmentContainer = view.findViewById(R.id.attachmentContainer)
        txtNoAttachments = view.findViewById(R.id.txtNoAttachments)
        spinnerStatus = view.findViewById(R.id.spinnerStatus)
        btnSaveStatus = view.findViewById(R.id.btnSaveStatus)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnUpload = view.findViewById(R.id.btnUploadFile)

        setupSpinner()

        loadBookingDetails(
            txtCompanyName, txtCompanyAddress, txtTaskRef, txtStatusPill,
            txtOriginDestination, txtWasteType, txtQuantity, txtPackaging, txtSpecialInstructions
        )

        btnUpload.setOnClickListener { filePickerLauncher.launch("*/*") }
        btnSaveStatus.setOnClickListener { saveStatus() }
        btnCancel.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        return view
    }

    private fun setupSpinner() {
        val statusOptions = resources.getStringArray(R.array.transporter_status_options)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = adapter
    }

    private fun displayUploadedFiles() {
        attachmentContainer.removeAllViews()

        if (uploadedFiles.isEmpty()) {
            txtNoAttachments.visibility = View.VISIBLE
            return
        }

        txtNoAttachments.visibility = View.GONE

        uploadedFiles.forEach { uri ->
            val txt = TextView(requireContext())
            txt.text = uri.lastPathSegment ?: uri.toString()
            attachmentContainer.addView(txt)
        }
    }

    // -------------------------------------------------------------
    // LOAD BOOKING
    // -------------------------------------------------------------
    private fun loadBookingDetails(
        txtCompanyName: TextView,
        txtCompanyAddress: TextView,
        txtTaskRef: TextView,
        txtStatusPill: TextView,
        txtOriginDestination: TextView,
        txtWasteType: TextView,
        txtQuantity: TextView,
        txtPackaging: TextView,
        txtSpecialInstructions: TextView
    ) {
        bookingId?.let { id ->
            db.collection("transport_bookings").document(id)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        // ---------------- TRANSPORTER MODE ----------------
                        bookingSource = BookingSource.TRANSPORT

                        txtCompanyName.text = doc.getString("serviceProviderCompany") ?: ""
                        txtCompanyAddress.text = ""
                        txtTaskRef.text = "Ref: ${doc.getString("bookingId") ?: id}"

                        // Robust: read either wasteStatus or bookingStatus (some writers use either)
                        val savedStatus = doc.getString("wasteStatus")
                            ?: doc.getString("bookingStatus")
                            ?: "Pending"

                        updateStatusPill(savedStatus)
                        applyDeliveredLock(savedStatus)

                        txtOriginDestination.text =
                            "${doc.getString("origin") ?: ""} → ${doc.getString("destination") ?: ""}"

                        txtWasteType.text = doc.getString("wasteType") ?: ""
                        txtQuantity.text = doc.getString("quantity") ?: ""
                        txtPackaging.text = doc.getString("packaging") ?: ""
                        txtSpecialInstructions.text = doc.getString("specialInstructions") ?: ""

                        uploadedFiles.clear()
                        val existing = doc.get("collectionProof") as? List<String> ?: emptyList()
                        uploadedFiles.addAll(existing.map { Uri.parse(it) })
                        displayUploadedFiles()

                        // spinner: case-insensitive matching and safe fallback
                        val options = resources.getStringArray(R.array.transporter_status_options)
                        val idx = options.indexOfFirst { it.equals(savedStatus, ignoreCase = true) }.let { if (it >= 0) it else 0 }
                        spinnerStatus.setSelection(idx)

                    } else {
                        // ---------------- TSD MODE ----------------
                        loadTsdBookingForTaskUpdate(
                            id, txtCompanyName, txtCompanyAddress, txtTaskRef,
                            txtStatusPill, txtOriginDestination, txtWasteType,
                            txtQuantity, txtPackaging, txtSpecialInstructions
                        )
                    }
                }
                .addOnFailureListener {
                    loadTsdBookingForTaskUpdate(
                        id, txtCompanyName, txtCompanyAddress, txtTaskRef,
                        txtStatusPill, txtOriginDestination, txtWasteType,
                        txtQuantity, txtPackaging, txtSpecialInstructions
                    )
                }
        }
    }

    // -------------------------------------------------------------
    // LOAD TSD BOOKING (WITH FIELD REMAPPING)
    // -------------------------------------------------------------
    private fun loadTsdBookingForTaskUpdate(
        id: String,
        txtCompanyName: TextView,
        txtCompanyAddress: TextView,
        txtTaskRef: TextView,
        txtStatusPill: TextView,
        txtOriginDestination: TextView,
        txtWasteType: TextView,
        txtQuantity: TextView,
        txtPackaging: TextView,
        txtSpecialInstructions: TextView
    ) {
        db.collection("tsd_bookings").document(id)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(requireContext(), "Booking not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                bookingSource = BookingSource.TSD
                val m = doc.data ?: emptyMap<String, Any>()

                fun s(key: String, alt: String = "") =
                    (m[key] as? String)?.trim().takeUnless { it.isNullOrEmpty() } ?: alt

                // ---------------------------------
                // UPDATE LABELS FOR TSD MODE ONLY (kept as-is)
                // ---------------------------------
                try {
                    val card = requireView().findViewById<View>(R.id.cardTaskInfo)
                    if (card is ViewGroup) {
                        fun walk(vg: ViewGroup) {
                            for (i in 0 until vg.childCount) {
                                val ch = vg.getChildAt(i)
                                if (ch is TextView) {
                                    val label = ch.text?.toString()?.trim() ?: ""
                                    when (label) {
                                        "Origin / Destination" -> ch.text = "Treatment Info"
                                        "Packaging" -> ch.text = "Amount (PHP)"
                                    }
                                }
                                if (ch is ViewGroup) walk(ch)
                            }
                        }
                        walk(card)
                    }
                } catch (_: Exception) {}

                // ---------------------------------
                // FIELDS (CHANGES: prefer tsdName; fallback to wasteType instead of "Unknown";
                // if tsdName equals wasteType then show timestamp in company slot)
                // ---------------------------------
                val facilityNameRaw = s("facilityName", s("facility", ""))
                val tsdNameRaw = s("tsdName", "")
                val wasteRaw = s("wasteType", s("treatmentInfo", s("treatment", s("waste", ""))))
                val bookingRef = s("refNumber", s("bookingId", id))

                // choose timestamp if needed (preferred field 'timestamp', fallback 'dateCreated')
                val tsCandidate = (m["timestamp"] as? com.google.firebase.Timestamp)
                    ?: (m["dateCreated"] as? com.google.firebase.Timestamp)
                    ?: (m["confirmedAt"] as? com.google.firebase.Timestamp)
                val tsDisplay = tsCandidate?.toDate()?.let {
                    android.text.format.DateFormat.format("MMM dd, yyyy hh:mm a", it).toString()
                } ?: ""

                // decide what to show in company name:
                // Prefer tsdName if it's present and not equal to waste type.
                // If tsdName is exactly the waste type (e.g. "Food Waste"), show the timestamp instead.
                // If none of those, fallback to facilityName, then to waste type, then to "Unknown".
                val companyToShow = when {
                    tsdNameRaw.isNotBlank() && !tsdNameRaw.equals(wasteRaw, ignoreCase = true) -> tsdNameRaw
                    tsdNameRaw.isNotBlank() && tsdNameRaw.equals(wasteRaw, ignoreCase = true) && tsDisplay.isNotBlank() -> tsDisplay
                    facilityNameRaw.isNotBlank() -> facilityNameRaw
                    wasteRaw.isNotBlank() -> wasteRaw
                    else -> "Unknown"
                }

                txtCompanyName.text = companyToShow
                txtCompanyAddress.text = s("location", "")
                txtTaskRef.text = "Ref: $bookingRef"

                val status = s("status", s("bookingStatus", "Pending"))
                updateStatusPill(status)
                applyDeliveredLock(status)

                val treatment = s("treatmentInfo", s("treatment", s("notes", "-")))
                txtOriginDestination.text = treatment.ifEmpty { "-" }

                // waste display should always show the waste type (if exists), otherwise treatment
                val waste = if (wasteRaw.isNotBlank()) wasteRaw else treatment
                txtWasteType.text = if (waste.isNotEmpty()) waste else "-"

                txtQuantity.text = when (val q = m["quantity"]) {
                    is Number -> q.toString()
                    is String -> q
                    else -> ""
                }

                // Payment mapping (keeps original behavior)
                val total = (m["totalPayment"] as? Number)?.toDouble()
                val rate = (m["rate"] as? Number)?.toDouble()
                txtPackaging.text =
                    when {
                        total != null -> "₱ ${"%,.2f".format(total)}"
                        rate != null -> "₱ ${"%,.2f".format(rate)}"
                        else -> s("amount", "-")
                    }

                txtSpecialInstructions.text = s("specialInstructions", s("notes", "-"))

                // Attachments (keeps original behavior)
                uploadedFiles.clear()
                (m["previousRecordUrl"] as? String)?.let { if (it.isNotBlank()) uploadedFiles.add(Uri.parse(it)) }
                (m["certificateUrl"] as? String)?.let { if (it.isNotBlank()) uploadedFiles.add(Uri.parse(it)) }
                val cp = m["collectionProof"]
                if (cp is List<*>) cp.mapNotNull { it as? String }.forEach { uploadedFiles.add(Uri.parse(it)) }
                displayUploadedFiles()

                // spinner selection (keeps original behavior)
                val options = resources.getStringArray(R.array.transporter_status_options)
                spinnerStatus.setSelection(options.indexOf(status).coerceAtLeast(0))
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load booking.", Toast.LENGTH_SHORT).show()
            }
    }




    // -------------------------------------------------------------
    // SAVE STATUS + FILES
    // -------------------------------------------------------------
    // -------------------------------------------------------------
// SAVE STATUS + FILES (UPDATED: no optimistic UI lock; only lock after successful write)
// -------------------------------------------------------------
    private fun saveStatus() {
        val newStatus = spinnerStatus.selectedItem.toString().trim()
        val id = bookingId ?: return

        val collectionName =
            if (bookingSource == BookingSource.TSD) "tsd_bookings"
            else "transport_bookings"

        // Prepare update map
        val updateMap = mutableMapOf<String, Any>()
        if (collectionName == "transport_bookings") {
            updateMap["wasteStatus"] = newStatus
            // keep bookingStatus in sync so other screens that check bookingStatus behave correctly
            updateMap["bookingStatus"] = newStatus
        } else {
            updateMap["status"] = newStatus
            updateMap["bookingStatus"] = newStatus
        }

        // Disable controls while saving to prevent double clicks
        btnSaveStatus.isEnabled = false
        spinnerStatus.isEnabled = false

        val newFiles = uploadedFiles.filter { it.scheme == "content" || it.scheme == "file" }
        val oldUrls = uploadedFiles.filter { it.scheme == "https" }.map { it.toString() }

        fun finishWithSuccess() {
            // Only now update the pill and possibly lock UI
            updateStatusPill(newStatus)
            applyDeliveredLock(newStatus)

            Toast.makeText(requireContext(), "Update saved!", Toast.LENGTH_SHORT).show()
            btnSaveStatus.isEnabled = true
        }

        fun finishWithFailure() {
            Toast.makeText(requireContext(), "Failed to save update!", Toast.LENGTH_SHORT).show()
            // re-enable controls so user can retry
            btnSaveStatus.isEnabled = true
            spinnerStatus.isEnabled = true
        }

        if (newFiles.isNotEmpty()) {
            // Upload new files first
            val uploadTasks = newFiles.map { uri ->
                // use timestamp to avoid collisions
                val dest = "$collectionName/$id/booking_proofs/${System.currentTimeMillis()}_${uri.lastPathSegment}"
                val ref = storage.reference.child(dest)
                ref.putFile(uri).continueWithTask { t ->
                    if (!t.isSuccessful) throw t.exception ?: Exception("Upload failed")
                    ref.downloadUrl
                }
            }

            com.google.android.gms.tasks.Tasks.whenAllSuccess<android.net.Uri>(uploadTasks)
                .addOnSuccessListener { uris ->
                    val merged = oldUrls + uris.map { it.toString() }

                    if (collectionName == "transport_bookings") {
                        updateMap["collectionProof"] = merged
                    } else {
                        updateMap["previousRecordUrl"] = merged.firstOrNull() ?: ""
                        updateMap["collectionProof"] = merged
                    }

                    // persist after uploads
                    db.collection(collectionName).document(id)
                        .update(updateMap)
                        .addOnSuccessListener {
                            finishWithSuccess()
                        }
                        .addOnFailureListener {
                            finishWithFailure()
                        }
                }
                .addOnFailureListener {
                    // upload failed — let user retry
                    finishWithFailure()
                }

        } else {
            // no new files — update immediately
            if (collectionName == "transport_bookings") {
                updateMap["collectionProof"] = oldUrls
            } else {
                updateMap["previousRecordUrl"] = oldUrls.firstOrNull() ?: ""
                updateMap["collectionProof"] = oldUrls
            }

            db.collection(collectionName).document(id)
                .update(updateMap)
                .addOnSuccessListener {
                    finishWithSuccess()
                }
                .addOnFailureListener {
                    finishWithFailure()
                }
        }
    }


    /** Map raw status strings to the pill text (case-insensitive, tolerant) */
    private fun updateStatusPill(status: String) {
        val s = status.trim().lowercase()
        txtStatusPill.text = when {
            s.contains("delivered") || s.contains("completed") -> "Delivered"
            s.contains("in transit") || s.contains("transit") -> "In Transit"
            s.contains("received") || s.contains("confirmed") -> "Confirmed"
            s.contains("treated") -> "Treated"
            s.contains("rejected") -> "Rejected"
            else -> "Pending"
        }
    }


    /** If status is a final/completed state -> lock UI (hide buttons + disable spinner)
     *  NOTE: transit/in transit is treated as final for transporter to match TSD behavior you requested.
     */
    private fun applyDeliveredLock(status: String) {
        val s = status.trim().lowercase()
        val isFinal = s.contains("delivered") ||
                s.contains("completed") ||
                s.contains("treated") ||
                s.contains("rejected")

        if (isFinal) {
            btnSaveStatus.visibility = View.GONE
            btnCancel.visibility = View.GONE
            btnUpload.visibility = View.GONE
            spinnerStatus.isEnabled = false
        } else {
            btnSaveStatus.visibility = View.VISIBLE
            btnCancel.visibility = View.VISIBLE
            btnUpload.visibility = View.VISIBLE
            spinnerStatus.isEnabled = true
        }
    }




    private fun updateBookingInFirestore(collection: String, id: String, map: Map<String, Any>) {
        db.collection(collection).document(id)
            .update(map)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Update saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save update!", Toast.LENGTH_SHORT).show()
            }
    }
}
