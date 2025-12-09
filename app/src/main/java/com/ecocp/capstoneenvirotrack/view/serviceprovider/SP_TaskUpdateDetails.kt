package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import androidx.core.net.toUri
import com.ecocp.capstoneenvirotrack.api.NotifyDeliveredRequest
import com.ecocp.capstoneenvirotrack.api.NotifyTreatmentDoneRequest
import com.ecocp.capstoneenvirotrack.api.RetrofitClient
import retrofit2.Call

class SP_TaskUpdateDetails : Fragment() {

    private var bookingId: String? = null
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val uploadedFiles = mutableListOf<Uri>()

    private lateinit var txtStatusPill: TextView
    private lateinit var txtNoAttachments: TextView
    private lateinit var attachmentContainer: LinearLayout
    private lateinit var transporterStatus: TextView
    private lateinit var progressBarHorizontal: ProgressBar
    private lateinit var btnSaveStatus: Button
    private lateinit var btnCancel: Button
    private lateinit var btnUpload: Button
    private lateinit var btnInTransit: Button
    private lateinit var btnDelivered: Button
    private lateinit var btnReceiveWaste: Button
    private lateinit var btnFinishTreatment: Button

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
        btnSaveStatus = view.findViewById(R.id.btnSaveStatus)
        transporterStatus = view.findViewById(R.id.transporterStatus)
        progressBarHorizontal = view.findViewById(R.id.progressBarHorizontal)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnUpload = view.findViewById(R.id.btnUploadFile)
        btnInTransit = view.findViewById(R.id.btnInTransit)
        btnDelivered = view.findViewById(R.id.btnDelivered)
        btnReceiveWaste = view.findViewById(R.id.btnReceiveWaste)
        btnFinishTreatment = view.findViewById(R.id.btnFinishTreatment)

        loadBookingDetails(
            txtCompanyName, txtCompanyAddress, txtTaskRef, txtStatusPill,
            txtOriginDestination, txtWasteType, txtQuantity, txtPackaging, txtSpecialInstructions
        )

        btnUpload.setOnClickListener { filePickerLauncher.launch("*/*") }
        btnSaveStatus.setOnClickListener { saveStatus() }
        btnCancel.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        btnInTransit.setOnClickListener { updateStatusDirectly("In Transit") }
        btnDelivered.setOnClickListener { updateStatusDirectly("Delivered") }
        btnReceiveWaste.setOnClickListener { updateTsdStatus("Received") }
        btnFinishTreatment.setOnClickListener { updateTsdStatus("Finish Treatment") }

        updateButtonVisibility()
        return view
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

                        val status = doc.getString("status") ?: "Confirmed"
                        updateStatusPill(savedStatus)
                        applyDeliveredLock(savedStatus)
                        Log.d("Status Logging ", "Status: $status")
                        transporterStatus.text = status.uppercase()
                        updateProgressBar(status)


                        txtOriginDestination.text =
                            "${doc.getString("origin") ?: ""} → ${doc.getString("destination") ?: ""}"

                        txtStatusPill.text = doc.getString("wasteStatus") ?: ""
                        txtWasteType.text = doc.getString("wasteType") ?: ""
                        txtQuantity.text = doc.getString("quantity") ?: ""
                        txtPackaging.text = doc.getString("packaging") ?: ""
                        txtSpecialInstructions.text = doc.getString("specialInstructions") ?: ""

                        uploadedFiles.clear()
                        val existing = doc.get("collectionProof") as? List<String> ?: emptyList()
                        uploadedFiles.addAll(existing.map { Uri.parse(it) })
                        displayUploadedFiles()


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
    @SuppressLint("UseKtx")
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
                } catch (_: Exception) {
                }

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
                    tsdNameRaw.isNotBlank() && !tsdNameRaw.equals(
                        wasteRaw,
                        ignoreCase = true
                    ) -> tsdNameRaw

                    tsdNameRaw.isNotBlank() && tsdNameRaw.equals(
                        wasteRaw,
                        ignoreCase = true
                    ) && tsDisplay.isNotBlank() -> tsDisplay

                    facilityNameRaw.isNotBlank() -> facilityNameRaw
                    wasteRaw.isNotBlank() -> wasteRaw
                    else -> "Unknown"
                }

                txtCompanyName.text = companyToShow
                txtCompanyAddress.text = s("location", "")
                txtTaskRef.text = "Ref: $bookingRef"

                val generatorId = doc.getString("generatorId")?:""
                db.collection("transport_bookings")
                    .whereEqualTo("pcoId",generatorId)
                    .get()
                    .addOnSuccessListener{wasteDoc->
                        val wasteStatus = wasteDoc.documents[0].getString("wasteStatus")
                        Log.d("WasteStatus", "Waste status: $wasteStatus")
                        txtStatusPill.text = wasteStatus
                    }

                val status = s("status", s("bookingStatus", "Pending"))
                val transportStatus = s("status", "Confirmed")
                updateStatusPill(status)
                applyDeliveredLock(status)
                Log.d("Status Logging ", "Status: $transportStatus")
                transporterStatus.text = transportStatus.uppercase()
                updateProgressBar(transportStatus)

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
                (m["previousRecordUrl"] as? String)?.let {
                    if (it.isNotBlank()) uploadedFiles.add(
                        Uri.parse(it)
                    )
                }
                (m["certificateUrl"] as? String)?.let {
                    if (it.isNotBlank()) uploadedFiles.add(
                        it.toUri()
                    )
                }
                val cp = m["collectionProof"]
                if (cp is List<*>) cp.mapNotNull { it as? String }
                    .forEach { uploadedFiles.add(Uri.parse(it)) }
                displayUploadedFiles()

            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load booking.", Toast.LENGTH_SHORT)
                    .show()
            }
    }


    // -------------------------------------------------------------
    // SAVE STATUS + FILES
    // -------------------------------------------------------------
    // -------------------------------------------------------------
// SAVE STATUS + FILES (UPDATED: no optimistic UI lock; only lock after successful write)
// -------------------------------------------------------------
    private fun saveStatus() {
        val id = bookingId ?: return

        val collectionName =
            if (bookingSource == BookingSource.TSD) "tsd_bookings"
            else "transport_bookings"
        val updateMap = mutableMapOf<String, Any>()

        // Disable controls while saving to prevent double clicks
        btnSaveStatus.isEnabled = false

        val newFiles = uploadedFiles.filter { it.scheme == "content" || it.scheme == "file" }
        val oldUrls = uploadedFiles.filter { it.scheme == "https" }.map { it.toString() }

        fun finishWithSuccess() {
            Toast.makeText(requireContext(), "Update saved!", Toast.LENGTH_SHORT).show()
            btnSaveStatus.isEnabled = true
        }

        fun finishWithFailure() {
            Toast.makeText(requireContext(), "Failed to save update!", Toast.LENGTH_SHORT).show()
            // re-enable controls so user can retry
            btnSaveStatus.isEnabled = true
        }

        if (newFiles.isNotEmpty()) {
            // Upload new files first
            val uploadTasks = newFiles.map { uri ->
                // use timestamp to avoid collisions
                val dest =
                    "$collectionName/$id/booking_proofs/${System.currentTimeMillis()}_${uri.lastPathSegment}"
                val ref = storage.reference.child(dest)
                ref.putFile(uri).continueWithTask { t ->
                    if (!t.isSuccessful) throw t.exception ?: Exception("Upload failed")
                    ref.downloadUrl
                }
            }

            Tasks.whenAllSuccess<Uri>(uploadTasks)
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
        val statusPair: Pair<String, String> = when {
            s.contains("delivered") || s.contains("completed") -> Pair("Delivered", "#4CAF50")
            s.contains("in transit") || s.contains("transit") -> Pair("In Transit", "#FF9800")
            s.contains("confirmed") -> Pair("Confirmed", "#2196F3")
            s.contains("received") -> Pair("Received", "#ABCDEF")
            s.contains("treated") || s.contains("started") -> Pair("Treated", "#9C27B0")
            s.contains("rejected") -> Pair("Rejected", "#F44336")
            else -> Pair("Pending", "#9E9E9E")
        }
        txtStatusPill.text = statusPair.first
        txtStatusPill.background.setTint(android.graphics.Color.parseColor(statusPair.second))
    }


    /** If status is a final/completed state -> lock UI (hide buttons + disable spinner)
     *  NOTE: transit/in transit is treated as final for transporter to match TSD behavior you requested.
     */
    private fun updateProgressBar(status: String) {
        val s = status.trim().lowercase()
        if (bookingSource == BookingSource.TSD && (s.contains("in transit") || s.contains("transit"))) {
            progressBarHorizontal.visibility = View.VISIBLE
        } else {
            progressBarHorizontal.visibility = View.GONE
        }
    }

    private fun updateButtonVisibility() {
        if (bookingSource == BookingSource.TRANSPORT) {
            btnInTransit.visibility = View.VISIBLE
            btnDelivered.visibility = View.VISIBLE
            btnCancel.visibility = View.VISIBLE
            btnSaveStatus.visibility = View.GONE
        } else {
            btnInTransit.visibility = View.GONE
            btnDelivered.visibility = View.GONE
            btnSaveStatus.visibility = View.GONE
        }
    }

    private fun updateTsdStatus(newStatus: String) {
        val id = bookingId ?: return
        val updateMap = mutableMapOf<String, Any>("wasteStatus" to newStatus)

        btnReceiveWaste.isEnabled = false
        btnFinishTreatment.isEnabled = false

        db.collection("tsd_bookings").document(id)
            .get()
            .addOnSuccessListener { tsdDoc ->

                val generatorId = tsdDoc.getString("generatorId") ?: ""
                val tsdId = tsdDoc.getString("tsdId") ?: ""   // 👈 IMPORTANT

                db.collection("transport_bookings")
                    .whereEqualTo("pcoId", generatorId)
                    .get()
                    .addOnSuccessListener { wasteQuery ->
                        if (wasteQuery.isEmpty) {
                            Toast.makeText(requireContext(), "No linked transport booking found", Toast.LENGTH_SHORT).show()
                            btnReceiveWaste.isEnabled = true
                            btnFinishTreatment.isEnabled = true
                            return@addOnSuccessListener
                        }

                        val wasteDocId = wasteQuery.documents[0].id
                        db.collection("transport_bookings").document(wasteDocId)
                            .update(updateMap)
                            .addOnSuccessListener {

                                Toast.makeText(requireContext(), "Status updated to $newStatus", Toast.LENGTH_SHORT).show()

                                // -------------------------------------------------
                                //  ✅ If TSD finishes treatment → set available again
                                // -------------------------------------------------
                                if (newStatus == "Finish Treatment") {
                                    db.collection("service_providers")
                                        .document(tsdId)
                                        .update("availabilityStatus", "available")
                                        .addOnSuccessListener {
                                            Log.d("TSD", "TSD is now AVAILABLE again")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("TSD", "Failed to update availability: ${e.message}")
                                        }
                                }

                                // ------------------- 🔔 Notify PCO -------------------
                                val request = NotifyTreatmentDoneRequest(
                                    receiverId = generatorId,
                                    bookingId = id
                                )

                                RetrofitClient.instance.notifyTreatmentDone(request)
                                    .enqueue(object : retrofit2.Callback<Void> {
                                        override fun onResponse(call: Call<Void>, response: retrofit2.Response<Void>) {
                                            if (response.isSuccessful) {
                                                Log.d("NOTIF", "PCO notified: treatment done")
                                            } else {
                                                Log.e("NOTIF", "Failed to notify PCO: ${response.code()}")
                                            }
                                        }

                                        override fun onFailure(call: Call<Void>, t: Throwable) {
                                            Log.e("NOTIF", "Error notifying PCO: ${t.message}")
                                        }
                                    })

                                btnReceiveWaste.isEnabled = true
                                btnFinishTreatment.isEnabled = true
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to update status", Toast.LENGTH_SHORT).show()
                                btnReceiveWaste.isEnabled = true
                                btnFinishTreatment.isEnabled = true
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to fetch transport booking", Toast.LENGTH_SHORT).show()
                        btnReceiveWaste.isEnabled = true
                        btnFinishTreatment.isEnabled = true
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to fetch TSD booking", Toast.LENGTH_SHORT).show()
                btnReceiveWaste.isEnabled = true
                btnFinishTreatment.isEnabled = true
            }
    }



    private fun updateStatusDirectly(newStatus: String) {
        val id = bookingId ?: return
        val updateMap = mutableMapOf<String, Any>("status" to newStatus)

        btnInTransit.isEnabled = false
        btnDelivered.isEnabled = false

        db.collection("transport_bookings").document(id).get()
            .addOnSuccessListener { transportDoc ->
                if (!transportDoc.exists()) {
                    Log.e("SP_TaskUpdateDetails", "Transport booking not found")
                    Toast.makeText(requireContext(), "Transport booking not found", Toast.LENGTH_SHORT).show()
                    btnInTransit.isEnabled = true
                    btnDelivered.isEnabled = true
                    return@addOnSuccessListener
                }

                val pcoId = transportDoc.getString("pcoId") ?: ""
                val transporterId = transportDoc.getString("transporterId") ?: ""

                if (pcoId.isBlank()) {
                    Log.e("SP_TaskUpdateDetails", "No pcoId found in transport booking")
                    Toast.makeText(requireContext(), "Cannot link to TSD booking", Toast.LENGTH_SHORT).show()
                    btnInTransit.isEnabled = true
                    btnDelivered.isEnabled = true
                    return@addOnSuccessListener
                }

                db.collection("tsd_bookings")
                    .whereEqualTo("generatorId", pcoId)
                    .get()
                    .addOnSuccessListener { tsdQuerySnap ->
                        if (tsdQuerySnap.isEmpty) {
                            Log.e("SP_TaskUpdateDetails","No TSD booking found with generatorId: $pcoId")
                            Toast.makeText(requireContext(),"Linked TSD booking not found",Toast.LENGTH_SHORT).show()
                            btnInTransit.isEnabled = true
                            btnDelivered.isEnabled = true
                            return@addOnSuccessListener
                        }

                        val tsdDocId = tsdQuerySnap.documents[0].id

                        val transportUpdate = db.collection("transport_bookings").document(id)
                            .update(updateMap)

                        val tsdUpdate = db.collection("tsd_bookings").document(tsdDocId)
                            .update(updateMap)

                        Tasks.whenAll(transportUpdate, tsdUpdate)
                            .addOnSuccessListener {
                                transporterStatus.text = newStatus.uppercase()
                                updateStatusPill(newStatus)
                                updateProgressBar(newStatus)
                                applyDeliveredLock(newStatus)

                                Toast.makeText(requireContext(),"Status updated to $newStatus",Toast.LENGTH_SHORT).show()

                                // ------------------- 🔔 Notify PCO -------------------
                                val request = NotifyDeliveredRequest(
                                    receiverId = pcoId,
                                    bookingId = id
                                )
                                RetrofitClient.instance.notifyDelivered(request)
                                    .enqueue(object : retrofit2.Callback<Void> {
                                        override fun onResponse(call: Call<Void>, response: retrofit2.Response<Void>) {
                                            Log.d("NOTIF", "PCO notified: waste delivered")
                                        }

                                        override fun onFailure(call: Call<Void>, t: Throwable) {
                                            Log.e("NOTIF", "Error notifying PCO: ${t.message}")
                                        }
                                    })

                                // --------------------------------------------------------
                                // ✅ Make transporter AVAILABLE again if delivery is finished
                                // --------------------------------------------------------
                                if (newStatus.equals("Delivered", ignoreCase = true)) {
                                    db.collection("service_providers")
                                        .document(transporterId)
                                        .update("availabilityStatus", "available")
                                        .addOnSuccessListener {
                                            Log.d("SP_TaskUpdateDetails", "Transporter availability restored → available")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("SP_TaskUpdateDetails", "Failed to restore availability: ${e.message}")
                                        }
                                }

                                btnInTransit.isEnabled = true
                                btnDelivered.isEnabled = true
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("SP_TaskUpdateDetails", "TSD query failed", e)
                        Toast.makeText(requireContext(),"Failed to find TSD booking",Toast.LENGTH_SHORT).show()
                        btnInTransit.isEnabled = true
                        btnDelivered.isEnabled = true
                    }
            }
            .addOnFailureListener { e ->
                Log.e("SP_TaskUpdateDetails", "Transport fetch failed", e)
                Toast.makeText(requireContext(),"Failed to load transport booking",Toast.LENGTH_SHORT).show()
                btnInTransit.isEnabled = true
                btnDelivered.isEnabled = true
            }
    }


    private fun applyDeliveredLock(status: String) {
        if (bookingSource == BookingSource.TRANSPORT) {
            btnSaveStatus.visibility = View.GONE
            btnUpload.visibility = View.GONE
            btnInTransit.visibility = View.VISIBLE
            btnDelivered.visibility = View.VISIBLE
            btnCancel.visibility = View.VISIBLE
        } else {
            btnInTransit.visibility = View.GONE
            btnDelivered.visibility = View.GONE
            btnSaveStatus.visibility = View.GONE
            btnUpload.visibility = View.VISIBLE
            btnCancel.visibility = View.VISIBLE
            btnReceiveWaste.visibility = View.VISIBLE
            btnFinishTreatment.visibility = View.VISIBLE
        }
        updateProgressBar(status)
    }
}