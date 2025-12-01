package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class SP_ServiceRequestDetails : Fragment() {

    // Developer-provided fallback image file (local path)
    private val DEV_ATTACHMENT_URL = "/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png"

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val TAG = "SRDetails"

    // runtime role (determined at start)
    private var currentRole: String = "transporter" // default to transporter

    // Keep track of current tsd booking doc id and current attachment urls
    private var currentBookingId: String? = null
    private var currentCertificateUrl: String? = null
    private var currentPreviousRecordUrl: String? = null
    private var currentStatus: String? = null
    private var currentFacilityName: String? = null

    // ActivityResult launchers for picking files
    private val pickFileForCertificate = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadFileAndSave(it, "certificate") }
    }
    private val pickFileForPreviousRecord = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadFileAndSave(it, "previousRecord") }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sp_service_request_details, container, false)

        // bookingId passed by adapter (preferred)
        val bookingId = arguments?.getString("bookingId") ?: arguments?.getString("id")

        // detect role first, then load booking accordingly
        detectRoleAndLoadBooking(bookingId, view)

        // Buttons (existing)
        val btnAccept = view.findViewById<Button>(R.id.btnAccept)
        val btnReject = view.findViewById<Button>(R.id.btnReject)

        // New TSD-only buttons (make sure they exist in layout)
        val btnUploadCertificate = view.findViewById<Button?>(R.id.btnUploadCertificate)
        val btnUploadPreviousRecord = view.findViewById<Button?>(R.id.btnUploadPreviousRecord)
        val btnUpdateStatus = view.findViewById<Button?>(R.id.btnUpdateStatus)

        // -------------------------
        // New: robust role resolver usage for clicks
        // -------------------------
        btnAccept.setOnClickListener {
            Log.d(TAG, "btnAccept CLICKED — resolving role...")

            val id = bookingId ?: currentBookingId
            Log.d(TAG, "btnAccept bookingId = $id")

            if (id.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Missing booking id", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "btnAccept FAILED: bookingId is NULL")
                return@setOnClickListener
            }

            // Disable while we decide + act
            btnAccept.isEnabled = false
            btnReject.isEnabled = false
            val prevAcceptText = btnAccept.text

            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Please sign in", Toast.LENGTH_SHORT).show()
                btnAccept.isEnabled = true
                btnReject.isEnabled = true
                return@setOnClickListener
            }

            // Optional debug dumps (temporary)
            uid.let {
                db.collection("users").document(it).get().addOnSuccessListener { d -> Log.d(TAG, "users doc at click: ${d.data}") }
                db.collection("service_providers").document(it).get().addOnSuccessListener { d -> Log.d(TAG, "service_providers doc at click: ${d.data}") }
            }
            db.collection("tsd_bookings").document(id).get().addOnSuccessListener { b -> Log.d(TAG, "booking doc at click: ${b.data}") }

            // Resolve role then act
            fetchRoleForCurrentUser { role ->
                currentRole = role
                Log.d(TAG, "btnAccept USING ROLE = $role")
                if (role.contains("tsd")) {
                    btnAccept.text = "Receiving..."
                    handleTsdAccept(id, btnAccept, btnReject, prevAcceptText.toString())
                } else {
                    btnAccept.text = "Accepting..."
                    handleTransporterAccept(id, btnAccept, btnReject, prevAcceptText.toString())
                }
            }
        }

        // Reject / Treat behaviour (robust: re-check role at click time)
        btnReject.setOnClickListener {
            Log.d(TAG, "btnReject CLICKED (role=$currentRole)")

            val id = bookingId ?: currentBookingId
            Log.d(TAG, "btnReject bookingId = $id")

            if (id.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Missing booking id", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "btnReject FAILED: bookingId is NULL")
                return@setOnClickListener
            }

            // Visual state → disable while processing
            btnAccept.isEnabled = false
            btnReject.isEnabled = false
            val prevRejectText = btnReject.text

            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Please sign in", Toast.LENGTH_SHORT).show()
                btnAccept.isEnabled = true
                btnReject.isEnabled = true
                return@setOnClickListener
            }

            // Optional debug dumps (temporary)
            uid.let {
                db.collection("users").document(it).get().addOnSuccessListener { d -> Log.d(TAG, "users doc at click: ${d.data}") }
                db.collection("service_providers").document(it).get().addOnSuccessListener { d -> Log.d(TAG, "service_providers doc at click: ${d.data}") }
            }
            db.collection("tsd_bookings").document(id).get().addOnSuccessListener { b -> Log.d(TAG, "booking doc at click: ${b.data}") }

            // Resolve role then act
            fetchRoleForCurrentUser { role ->
                currentRole = role
                Log.d(TAG, "btnReject resolved role = $role")
                if (role.contains("tsd")) {
                    btnReject.text = "Treating..."
                    handleTsdReject(id, btnAccept, btnReject, prevRejectText.toString())
                } else {
                    btnReject.text = "Rejecting..."
                    handleTransporterReject(id, btnAccept, btnReject, prevRejectText.toString())
                }
            }
        }

        // TSD-only actions: upload certificate
        btnUploadCertificate?.setOnClickListener {
            if (currentRole == "tsd" || currentRole == "tsdfacility") {
                // open file picker (PDF/images)
                pickFileForCertificate.launch("*/*")
            } else {
                Toast.makeText(requireContext(), "Upload available only for TSD accounts", Toast.LENGTH_SHORT).show()
            }
        }

        // TSD-only: upload previous record
        btnUploadPreviousRecord?.setOnClickListener {
            if (currentRole == "tsd" || currentRole == "tsdfacility") {
                pickFileForPreviousRecord.launch("*/*")
            } else {
                Toast.makeText(requireContext(), "Upload available only for TSD accounts", Toast.LENGTH_SHORT).show()
            }
        }

        // TSD-only: update status (choose next)
        btnUpdateStatus?.setOnClickListener {
            if (currentRole == "tsd" || currentRole == "tsdfacility") {
                val id = bookingId ?: currentBookingId
                if (id.isNullOrBlank()) {
                    Toast.makeText(requireContext(), "Missing booking id", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                showStatusUpdateDialog(id, currentStatus ?: "Pending")
            } else {
                Toast.makeText(requireContext(), "Status update available only for TSD accounts", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    /* -------------------------
       Role detection & binding
       (unchanged logic - preserved from your version)
       ------------------------- */

    private fun extractStringFieldSafely(docData: Map<String, Any>?, vararg possibleKeys: String): String? {
        if (docData == null) return null
        for (k in possibleKeys) {
            if (docData.containsKey(k)) {
                val v = docData[k]
                if (v is String && v.isNotBlank()) return v
            }
        }
        return null
    }

    private fun fetchRoleForCurrentUser(callback: (role: String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            Log.w(TAG, "fetchRole: no signed-in user")
            callback("transporter")
            return
        }

        // Try users/{uid} first
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val userMap = userDoc.data
                val roleCandidate = extractStringFieldSafely(userMap,
                    "role", "Role", "userRole", "accountRole", "roleType")
                val roleRaw = roleCandidate?.lowercase()?.trim()

                if (!roleRaw.isNullOrBlank()) {
                    Log.d(TAG, "fetchRole: found role in users -> $roleRaw")
                    callback(if (roleRaw.contains("tsd")) "tsd" else "transporter")
                } else {
                    // fallback to service_providers; check multiple possible keys
                    db.collection("service_providers").document(uid).get()
                        .addOnSuccessListener { spDoc ->
                            val spMap = spDoc.data
                            val spCandidate = extractStringFieldSafely(spMap,
                                "providerType", "provider_type", "provider", "role", "Role", "type", "accountType")
                            val spRaw = spCandidate?.lowercase()?.trim() ?: ""
                            Log.d(TAG, "fetchRole: service_providers candidate = $spRaw")
                            callback(if (spRaw.contains("tsd")) "tsd" else "transporter")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "fetchRole: service_providers read failed: ${e.message}")
                            callback("transporter")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "fetchRole: users read failed: ${e.message} - trying service_providers")
                db.collection("service_providers").document(uid).get()
                    .addOnSuccessListener { spDoc ->
                        val spMap = spDoc.data
                        val spCandidate = extractStringFieldSafely(spMap,
                            "providerType", "provider_type", "provider", "role", "Role", "type", "accountType")
                        val spRaw = spCandidate?.lowercase()?.trim() ?: ""
                        callback(if (spRaw.contains("tsd")) "tsd" else "transporter")
                    }
                    .addOnFailureListener {
                        Log.w(TAG, "fetchRole: both user/service_providers reads failed")
                        callback("transporter")
                    }
            }
    }

    private fun detectRoleAndLoadBooking(bookingId: String?, view: View) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            Log.w(TAG, "User not signed in, defaulting to transporter behaviour")
            bookingId?.let { loadBookingFromTransportOrBundle(it, view) } ?: bindFromBundle(view)
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val roleRaw = userDoc.getString("role")?.trim() ?: ""
                val role = roleRaw.lowercase()
                currentRole = role
                Log.d(TAG, "Detected role = $currentRole")

                // treat any role that contains "tsd" as TSD
                val isTsd = role.contains("tsd")

                if (isTsd) {
                    view.findViewById<Button>(R.id.btnAccept).text = "Receive"
                    view.findViewById<Button>(R.id.btnReject).text = "Treat"
                    if (!bookingId.isNullOrBlank()) {
                        loadTsdBookingFromFirestore(bookingId, view)
                    } else {
                        bindFromBundle(view)
                    }
                } else {
                    // fallback to service_providers if user doc doesn't indicate tsd
                    db.collection("service_providers").document(uid).get()
                        .addOnSuccessListener { spDoc ->
                            val providerType = spDoc.getString("providerType")?.lowercase()?.trim() ?: ""
                            val providerIsTsd = providerType.contains("tsd")
                            if (providerIsTsd) {
                                currentRole = "tsd"
                                view.findViewById<Button>(R.id.btnAccept).text = "Receive"
                                view.findViewById<Button>(R.id.btnReject).text = "Treat"
                                if (!bookingId.isNullOrBlank()) {
                                    loadTsdBookingFromFirestore(bookingId, view)
                                } else {
                                    bindFromBundle(view)
                                }
                            } else {
                                currentRole = "transporter"
                                view.findViewById<Button>(R.id.btnAccept).text = "Accept"
                                view.findViewById<Button>(R.id.btnReject).text = "Reject"
                                if (!bookingId.isNullOrBlank()) {
                                    loadBookingFromFirestore(bookingId, view)
                                } else {
                                    bindFromBundle(view)
                                }
                            }
                        }
                        .addOnFailureListener {
                            // fail safe: treat as transporter
                            currentRole = "transporter"
                            view.findViewById<Button>(R.id.btnAccept).text = "Accept"
                            view.findViewById<Button>(R.id.btnReject).text = "Reject"
                            if (!bookingId.isNullOrBlank()) {
                                loadBookingFromFirestore(bookingId, view)
                            } else {
                                bindFromBundle(view)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to read users/{uid} role: ${e.message}. Falling back to transporter")
                currentRole = "transporter"
                if (!bookingId.isNullOrBlank()) {
                    loadBookingFromTransportOrBundle(bookingId, view)
                } else {
                    bindFromBundle(view)
                }
            }
    }

    // ----------------------------
    // TSD booking binder (enhanced)
    // ----------------------------
    private fun loadTsdBookingFromFirestore(bookingId: String, view: View) {
        db.collection("tsd_bookings").document(bookingId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(requireContext(), "TSD booking not found", Toast.LENGTH_SHORT).show()
                    bindFromBundle(view)
                    return@addOnSuccessListener
                }

                val m = doc.data ?: emptyMap<String, Any>()
                Log.d(TAG, "tsd docId=${doc.id} data=$m") // FULL doc dump

                currentBookingId = doc.id
                currentCertificateUrl = (m["certificateUrl"] as? String)?.trim().orEmpty().ifEmpty { null }
                currentPreviousRecordUrl = (m["previousRecordUrl"] as? String)?.trim().orEmpty().ifEmpty { null }
                currentStatus = (m["status"] as? String)?.trim()?.ifEmpty { "Pending" }
                currentFacilityName = (m["facilityName"] as? String)?.trim()?.ifEmpty { null }

                fun s(key: String, alt: String = ""): String {
                    return (m[key] as? String)?.trim().takeUnless { it.isNullOrEmpty() } ?: alt
                }

                val companyName = s("facilityName", s("facility", "TSD Facility"))
                val serviceType = "TSD - ${s("treatmentInfo", "Treatment")}"
                val location = s("location", "N/A")

                val dateRequested = (m["dateCreated"] as? com.google.firebase.Timestamp)?.toDate()?.let {
                    android.text.format.DateFormat.format("MMM dd, yyyy hh:mm a", it).toString()
                } ?: s("preferredDate", "N/A")

                val contactNumber = s("contactNumber", "N/A")
                // NOTE: keep reading both keys; bookingStatus var is what UI uses
                val bookingStatus = s("status", "") .ifEmpty { s("bookingStatus", "Pending") }
                val notes = s("treatmentInfo", s("notes", "No additional notes"))

                val quantity = when (val q = m["quantity"]) {
                    is String -> q
                    is Number -> q.toString()
                    else -> ""
                }

                val previousRecordUrl = s("previousRecordUrl", "")
                val certificateUrl = s("certificateUrl", "")

                // Bind UI
                view.findViewById<TextView>(R.id.txtCompanyName).text = companyName
                view.findViewById<TextView>(R.id.txtServiceType).text = serviceType
                view.findViewById<TextView>(R.id.txtLocation).text = location
                view.findViewById<TextView>(R.id.txtDateRequested).text = dateRequested
                view.findViewById<TextView>(R.id.txtContactPerson).text = contactNumber
                view.findViewById<TextView>(R.id.txtStatusPill).text = bookingStatus
                view.findViewById<TextView>(R.id.txtNotes).text = notes

                view.findViewById<TextView>(R.id.txtWasteType).text = s("treatmentInfo", "-")
                view.findViewById<TextView>(R.id.txtQuantity).text = quantity.ifEmpty { "-" }
                view.findViewById<TextView>(R.id.txtPackaging).text = "-" // not present in tsd doc
                view.findViewById<TextView>(R.id.txtDestination).text = companyName
                view.findViewById<TextView>(R.id.txtAmount).text =
                    if ((m["totalPayment"] as? Number)?.toDouble() ?: 0.0 > 0.0)
                        "₱ ${"%,.2f".format((m["totalPayment"] as Number).toDouble())}" else "-"

                // Attachment: prefer previousRecord then certificate then fallback
                val firstAttachment = when {
                    previousRecordUrl.isNotBlank() -> previousRecordUrl
                    certificateUrl.isNotBlank() -> certificateUrl
                    else -> DEV_ATTACHMENT_URL
                }
                setupAttachmentUI(view, firstAttachment)

                // store attachments locally for open action
                currentCertificateUrl = certificateUrl.ifEmpty { null }
                currentPreviousRecordUrl = previousRecordUrl.ifEmpty { null }

                // ---------------------------
                // DECIDE VISIBILITY — robust
                // ---------------------------
                val btnAccept = view.findViewById<Button>(R.id.btnAccept)
                val btnReject = view.findViewById<Button>(R.id.btnReject)
                val btnUploadCertificate = view.findViewById<Button?>(R.id.btnUploadCertificate)
                val btnUploadPreviousRecord = view.findViewById<Button?>(R.id.btnUploadPreviousRecord)
                val btnUpdateStatus = view.findViewById<Button?>(R.id.btnUpdateStatus)

                // Gather status sources
                val docStatus = (m["status"] as? String)?.trim()
                val docBookingStatus = (m["bookingStatus"] as? String)?.trim()
                val uiStatusText = view.findViewById<TextView>(R.id.txtStatusPill).text?.toString()?.trim()

                Log.d(TAG, "DECIDER: doc.status='$docStatus', doc.bookingStatus='$docBookingStatus', uiStatusPill='$uiStatusText'")

                // normalize: choose any non-empty that indicates final state
                val effectiveStatus = listOfNotNull(docStatus, docBookingStatus, uiStatusText)
                    .firstOrNull { it.isNotBlank() }?.trim() ?: "Pending"

                val finalStates = setOf("confirmed", "treated", "rejected", "received", "completed")
                val nonActionable = finalStates.contains(effectiveStatus.lowercase())

                // Apply UI changes on main thread
                activity?.runOnUiThread {
                    if (nonActionable) {
                        btnAccept.visibility = View.GONE
                        btnReject.visibility = View.GONE
                        btnAccept.isEnabled = false
                        btnReject.isEnabled = false
                        Log.d(TAG, "DECIDER: HIDING buttons because effectiveStatus='$effectiveStatus'")
                    } else {
                        btnAccept.visibility = View.VISIBLE
                        btnReject.visibility = View.VISIBLE
                        btnAccept.isEnabled = true
                        btnReject.isEnabled = true
                        Log.d(TAG, "DECIDER: SHOWING buttons because effectiveStatus='$effectiveStatus'")
                    }

                    // TSD-only controls: keep previous owner logic if you want, else always visible
                    // Here we keep them visible (you can change to only owner if needed)
                    btnUploadCertificate?.visibility = View.VISIBLE
                    btnUploadPreviousRecord?.visibility = View.VISIBLE
                    btnUpdateStatus?.visibility = View.VISIBLE
                }

                // If certificate exists, clicking txtAttachments opens it
                val attachmentsText = view.findViewById<TextView>(R.id.txtAttachments)
                if (certificateUrl.isNotBlank() || previousRecordUrl.isNotBlank()) {
                    attachmentsText.setOnClickListener {
                        val urlToOpen = currentPreviousRecordUrl ?: currentCertificateUrl ?: ""
                        if (urlToOpen.isNotBlank()) openAttachment(urlToOpen)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed loading tsd booking", e)
                Toast.makeText(requireContext(), "Failed to load TSD booking: ${e.message}", Toast.LENGTH_LONG).show()
                bindFromBundle(view)
            }
    }

    override fun onResume() {
        super.onResume()
        // re-evaluate visibility after resume in case something else toggled it
        try {
            val root = view ?: return
            recheckTsdButtonsByStatusPill(root)
        } catch (ex: Exception) {
            Log.w(TAG, "onResume recheck failed: ${ex.message}")
        }
    }

    private fun recheckTsdButtonsByStatusPill(rootView: View) {
        val statusPill = rootView.findViewById<TextView?>(R.id.txtStatusPill)?.text?.toString()?.trim() ?: return
        val btnAccept = rootView.findViewById<Button?>(R.id.btnAccept)
        val btnReject = rootView.findViewById<Button?>(R.id.btnReject)
        val lower = statusPill.lowercase()
        val finalStates = setOf("confirmed", "treated", "rejected", "received", "completed")
        if (finalStates.contains(lower)) {
            btnAccept?.visibility = View.GONE
            btnReject?.visibility = View.GONE
            btnAccept?.isEnabled = false
            btnReject?.isEnabled = false
            Log.d(TAG, "onResume DECIDER: hid buttons based on statusPill='$statusPill'")
        } else {
            btnAccept?.visibility = View.VISIBLE
            btnReject?.visibility = View.VISIBLE
            btnAccept?.isEnabled = true
            btnReject?.isEnabled = true
            Log.d(TAG, "onResume DECIDER: showed buttons based on statusPill='$statusPill'")
        }
    }


    /* ----------------------------
       Generic helper to update booking status for any collection
       ---------------------------- */
    private fun updateBookingStatusGeneric(
        collectionName: String,
        bookingId: String,
        statusValue: String,
        actorUid: String,
        lifecycleField: String?,             // e.g. "confirmedAt" or "treatedAt" or null
        extraFields: Map<String, Any> = emptyMap(),
        callback: (Boolean) -> Unit
    ) {
        if (actorUid.isBlank()) {
            Log.e(TAG, "updateBookingStatusGeneric FAILED: actorUid is blank")
            callback(false); return
        }

        // Core status fields: write both "status" and "bookingStatus" to avoid mismatches
        val updates = mutableMapOf<String, Any>(
            "status" to statusValue,
            "bookingStatus" to statusValue,
            "statusUpdatedAt" to FieldValue.serverTimestamp(),
            "statusUpdatedBy" to actorUid
        )

        // Add the lifecycle timestamp field if provided (confirmedAt, treatedAt, etc.)
        lifecycleField?.let { updates[it] = FieldValue.serverTimestamp() }

        // Merge any caller-provided extra fields (e.g., treatmentInfo, rejectedBy, etc.)
        updates.putAll(extraFields)

        Log.d(TAG, "updateBookingStatusGeneric: collection=$collectionName id=$bookingId updates=$updates")

        db.collection(collectionName)
            .document(bookingId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Firestore UPDATE SUCCESS [$collectionName/$bookingId] -> $statusValue")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore UPDATE FAILED [$collectionName/$bookingId]: ${e.message}", e)
                callback(false)
            }
    }

    // ----------------------------
    // TRANSPORTER: accept (-> Confirmed) using generic helper
    // ----------------------------
    private fun acceptBooking(bookingId: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "acceptBooking() CALLED for bookingId=$bookingId")
        val uid = auth.currentUser?.uid ?: ""
        val extra = mapOf<String, Any>(
            "providerId" to uid,
            "assignedAt" to FieldValue.serverTimestamp()
        )
        updateBookingStatusGeneric(
            collectionName = "transport_bookings",
            bookingId = bookingId,
            statusValue = "Confirmed",
            actorUid = uid,
            lifecycleField = "confirmedAt",
            extraFields = extra,
            callback = callback
        )
    }

    // ----------------------------
    // TRANSPORTER: reject (-> Rejected) using generic helper
    // ----------------------------
    private fun rejectBooking(bookingId: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "rejectBooking() CALLED for bookingId=$bookingId")
        val uid = auth.currentUser?.uid ?: ""
        val extra = mapOf<String, Any>(
            "rejectedBy" to uid,
            "rejectedAt" to FieldValue.serverTimestamp()
        )
        updateBookingStatusGeneric(
            collectionName = "transport_bookings",
            bookingId = bookingId,
            statusValue = "Rejected",
            actorUid = uid,
            lifecycleField = "rejectedAt",
            extraFields = extra,
            callback = callback
        )
    }

    // ----------------------------
    // TSD: receive (-> Confirmed) using generic helper
    // ----------------------------
    private fun receiveTsdBooking(bookingId: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "receiveTsdBooking() CALLED for bookingId=$bookingId")
        val uid = auth.currentUser?.uid ?: ""
        updateBookingStatusGeneric(
            collectionName = "tsd_bookings",
            bookingId = bookingId,
            statusValue = "Confirmed",
            actorUid = uid,
            lifecycleField = "confirmedAt",
            extraFields = mapOf("confirmedBy" to uid),
            callback = callback
        )
    }

    // ----------------------------
    // TSD: treat (-> Treated) using generic helper
    // ----------------------------
    private fun treatTsdBooking(bookingId: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "treatTsdBooking() CALLED for bookingId=$bookingId")
        val uid = auth.currentUser?.uid ?: ""
        val extra = mapOf<String, Any>(
            "treatedBy" to uid,
            "treatmentInfo" to "Treated via mobile app by $uid"
        )
        updateBookingStatusGeneric(
            collectionName = "tsd_bookings",
            bookingId = bookingId,
            statusValue = "Treated",
            actorUid = uid,
            lifecycleField = "treatedAt",
            extraFields = extra,
            callback = callback
        )
    }

    /* ----------------------------
       Upload helpers (pick → upload → save URL to Firestore)
       ---------------------------- */
    private fun uploadFileAndSave(fileUri: Uri, type: String) {
        // type: "certificate" or "previousRecord"
        val bookingId = currentBookingId
        if (bookingId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Booking ID missing", Toast.LENGTH_SHORT).show()
            return
        }

        // create storage path: tsd_bookings/{bookingId}/{type}_{uuid}.pdf (or original name extension)
        val ext = (activity?.contentResolver?.getType(fileUri) ?: "application/octet-stream").substringAfterLast("/")
        val filename = "${type}_${UUID.randomUUID()}.$ext"
        val refPath = "tsd_bookings/$bookingId/$filename"
        val storageRef: StorageReference = storage.reference.child(refPath)

        Toast.makeText(requireContext(), "Uploading $type...", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Uploading file to $refPath")

        val uploadTask = storageRef.putFile(fileUri)
        uploadTask.addOnProgressListener { taskSnapshot ->
            val percent = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
            Log.d(TAG, "Upload progress: ${"%.1f".format(percent)}%")
        }.addOnSuccessListener {
            // get download URL
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val url = downloadUri.toString()
                Log.d(TAG, "Upload success, downloadUrl=$url")
                // Save to Firestore
                val fieldName = when (type) {
                    "certificate" -> "certificateUrl"
                    "previousRecord" -> "previousRecordUrl"
                    else -> "previousRecordUrl"
                }
                val updates = mapOf<String, Any>(
                    fieldName to url,
                    "${type}UploadedAt" to FieldValue.serverTimestamp(),
                    "${type}UploadedBy" to (auth.currentUser?.uid ?: "unknown")
                )
                db.collection("tsd_bookings").document(bookingId)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "$type uploaded", Toast.LENGTH_SHORT).show()
                        // refresh local state by reloading doc
                        loadTsdBookingFromFirestore(bookingId, requireView())
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed saving $type URL to Firestore: ${e.message}", e)
                        Toast.makeText(requireContext(), "Failed saving file info: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to obtain download URL: ${e.message}", e)
                Toast.makeText(requireContext(), "Upload succeeded but failed to get URL: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Upload failed: ${e.message}", e)
            Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /* ----------------------------
       Show status update dialog (TSD)
       ---------------------------- */
    private fun showStatusUpdateDialog(bookingId: String, currentStatus: String) {
        val allowedNext = when (currentStatus.lowercase()) {
            "pending", "pending payment", "received" -> listOf("Confirmed", "Processing")
            "confirmed" -> listOf("Processing", "Completed")
            "processing" -> listOf("Completed")
            else -> listOf("Confirmed", "Processing", "Completed")
        }


        val items = allowedNext.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Update status (current: $currentStatus)")
            .setItems(items) { dialog, which ->
                val chosen = items[which]
                // use generic helper to update status so both keys remain consistent
                val uid = auth.currentUser?.uid ?: ""
                updateBookingStatusGeneric(
                    collectionName = "tsd_bookings",
                    bookingId = bookingId,
                    statusValue = chosen,
                    actorUid = uid,
                    lifecycleField = when (chosen.lowercase()) {
                        "confirmed" -> "confirmedAt"
                        "processing" -> "processingAt"
                        "completed" -> "completedAt"
                        else -> null
                    },
                    extraFields = emptyMap(),
                    callback = { success ->
                        if (success) {
                            Toast.makeText(requireContext(), "Status updated to $chosen", Toast.LENGTH_SHORT).show()
                            // refresh doc
                            loadTsdBookingFromFirestore(bookingId, requireView())
                        } else {
                            Toast.makeText(requireContext(), "Failed to update status", Toast.LENGTH_LONG).show()
                        }
                    }
                )
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ----------------------------
    // Helper methods (transport + tsd) - pasted here (transport binding etc.)
    // ----------------------------

    // ----------------------------
    // Helper: load transport booking or fall back to bundle bindings
    // ----------------------------
    private fun loadBookingFromTransportOrBundle(bookingId: String, view: View) {
        db.collection("transport_bookings").document(bookingId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    bindFromBundle(view)
                    return@addOnSuccessListener
                }
                val m = doc.data ?: emptyMap<String, Any>()
                Log.d(TAG, "transport docId=${doc.id} data=$m")
                bindTransportDocToView(m, doc.id, view)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed loading transport booking: ${e.message}", e)
                bindFromBundle(view)
            }
    }

    // ----------------------------
    // Load transport booking by id (document) and bind
    // ----------------------------
    private fun loadBookingFromFirestore(bookingId: String, view: View) {
        db.collection("transport_bookings").document(bookingId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(requireContext(), "Booking not found", Toast.LENGTH_SHORT).show()
                    bindFromBundle(view)
                    return@addOnSuccessListener
                }
                val m = doc.data ?: emptyMap<String, Any>()
                Log.d(TAG, "docId=${doc.id} data=$m")
                bindTransportDocToView(m, doc.id, view)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed loading booking", e)
                Toast.makeText(requireContext(), "Failed to load booking: ${e.message}", Toast.LENGTH_LONG).show()
                bindFromBundle(view)
            }
    }

    // ----------------------------
    // Bind transport document map -> view (keeps your earlier logic)
    // ----------------------------
    private fun bindTransportDocToView(m: Map<String, Any>, docId: String, view: View) {
        fun s(key: String, alt: String = ""): String {
            val v = (m[key] as? String)?.trim()
            return if (!v.isNullOrEmpty()) v else alt
        }

        val companyName = s("serviceProviderCompany", s("companyName", "Unknown"))
        val wasteType = s("wasteType")
        val serviceType = if (wasteType.isNotBlank()) "Transport - $wasteType" else "Transport Booking"
        val origin = s("origin", s("pickupLocation", "N/A"))

        val dateRequested = (m["bookingDate"] as? com.google.firebase.Timestamp)?.toDate()?.let {
            android.text.format.DateFormat.format("MMM dd, yyyy hh:mm a", it).toString()
        } ?: (m["dateBooked"] as? com.google.firebase.Timestamp)?.toDate()?.let {
            android.text.format.DateFormat.format("MMM dd, yyyy hh:mm a", it).toString()
        } ?: "N/A"

        val providerContact = s("providerContact", s("contactNumber", "N/A"))
        val providerName = s("serviceProviderName", s("name", ""))
        val bookingStatus = s("bookingStatus", "Pending")
        val notes = s("specialInstructions", s("notes", "No additional notes"))

        val quantity = when (val q = m["quantity"]) {
            is String -> q
            is Number -> q.toString()
            else -> ""
        }

        val packaging = s("packaging", "")
        val destination = s("destination", s("dropoffLocation", ""))

        val transportPlanUrl = (m["transportPlanUrl"] as? String)?.takeIf { it.isNotBlank() }
        val storagePermitUrl = (m["storagePermitUrl"] as? String)?.takeIf { it.isNotBlank() }

        // Bind UI
        view.findViewById<TextView>(R.id.txtCompanyName).text = companyName
        view.findViewById<TextView>(R.id.txtServiceType).text = serviceType
        view.findViewById<TextView>(R.id.txtLocation).text = origin
        view.findViewById<TextView>(R.id.txtDateRequested).text = dateRequested
        view.findViewById<TextView>(R.id.txtContactPerson).text =
            if (providerName.isNotEmpty()) "$providerName — $providerContact" else providerContact
        view.findViewById<TextView>(R.id.txtStatusPill).text = bookingStatus
        view.findViewById<TextView>(R.id.txtNotes).text = notes

        view.findViewById<TextView>(R.id.txtWasteType).text = wasteType.ifEmpty { "-" }
        view.findViewById<TextView>(R.id.txtQuantity).text = quantity.ifEmpty { "-" }
        view.findViewById<TextView>(R.id.txtPackaging).text = packaging.ifEmpty { "-" }
        view.findViewById<TextView>(R.id.txtDestination).text = destination.ifEmpty { "-" }

        val firstAttachment = transportPlanUrl ?: storagePermitUrl ?: DEV_ATTACHMENT_URL
        setupAttachmentUI(view, firstAttachment)

        // --------------------------------------------------------
        // HIDE ACCEPT/REJECT IF CONFIRMED OR REJECTED
        // --------------------------------------------------------
        val btnAccept = view.findViewById<Button>(R.id.btnAccept)
        val btnReject = view.findViewById<Button>(R.id.btnReject)

        Log.d(TAG, "bookingStatus = $bookingStatus → evaluating button visibility")

        if (bookingStatus.equals("Confirmed", true) ||
            bookingStatus.equals("Rejected", true)) {

            btnAccept.visibility = View.GONE
            btnReject.visibility = View.GONE
            Log.d(TAG, "Buttons hidden because bookingStatus=$bookingStatus")
        } else {
            btnAccept.visibility = View.VISIBLE
            btnReject.visibility = View.VISIBLE
            Log.d(TAG, "Buttons visible (Pending state)")
        }
    }

    // ----------------------------
    // Bind from bundle (when no doc found) — keeps your existing bundle mapping
    // ----------------------------
    private fun bindFromBundle(view: View) {
        val companyName     = arguments?.getString("companyName")     ?: "Unknown"
        val serviceType     = arguments?.getString("serviceTitle")    ?: "Transport Booking"
        val location        = arguments?.getString("origin")          ?: "N/A"
        val dateRequested   = arguments?.getString("dateRequested")   ?: "N/A"
        val providerContact = arguments?.getString("providerContact") ?: "N/A"
        val providerName    = arguments?.getString("providerName")    ?: ""
        val bookingStatus   = arguments?.getString("bookingStatus")   ?: "Pending"
        val notes           = arguments?.getString("notes")           ?: "No additional notes"
        val attachment      = arguments?.getString("attachment")      ?: DEV_ATTACHMENT_URL

        view.findViewById<TextView>(R.id.txtCompanyName).text = companyName
        view.findViewById<TextView>(R.id.txtServiceType).text = serviceType
        view.findViewById<TextView>(R.id.txtLocation).text = location
        view.findViewById<TextView>(R.id.txtDateRequested).text = dateRequested

        view.findViewById<TextView>(R.id.txtContactPerson).text =
            if (providerName.isNotEmpty()) "$providerName — $providerContact" else providerContact

        view.findViewById<TextView>(R.id.txtStatusPill).text = bookingStatus
        view.findViewById<TextView>(R.id.txtNotes).text = notes

        setupAttachmentUI(view, attachment)
    }

    // ----------------------------
    // Attachment UI binder (keeps your existing UI behavior)
    // ----------------------------
    private fun setupAttachmentUI(view: View, attachmentPath: String) {
        val txtAttach = view.findViewById<TextView>(R.id.txtAttachments)
        txtAttach.text = attachmentPath.substringAfterLast("/")
        txtAttach.tag = attachmentPath

        val imgLogo = view.findViewById<ImageView>(R.id.imgCompanyLogo)
        try {
            Glide.with(this)
                .load(attachmentPath)
                .error(Glide.with(this).load(DEV_ATTACHMENT_URL))
                .into(imgLogo)
        } catch (ex: Exception) {
            Log.w(TAG, "Glide load failed, using fallback", ex)
            Glide.with(this).load(DEV_ATTACHMENT_URL).into(imgLogo)
        }

        txtAttach.setOnClickListener {
            openAttachment(attachmentPath)
        }

        val uploadTransportStatus = view.findViewById<TextView>(R.id.txtUploadTransportPlanStatus)
        val uploadStorageStatus = view.findViewById<TextView>(R.id.txtUploadStoragePermitStatus)

        val uploadedTransport = attachmentPath.contains("transportPlan", ignoreCase = true) || attachmentPath.startsWith("http")
        if (uploadedTransport) {
            uploadTransportStatus.text = "Transport Plan: Uploaded"
            uploadTransportStatus.visibility = View.VISIBLE
        } else {
            uploadTransportStatus.visibility = View.GONE
        }

        if (attachmentPath.contains("storagePermit", ignoreCase = true)) {
            uploadStorageStatus.text = "Storage Permit: Uploaded"
            uploadStorageStatus.visibility = View.VISIBLE
        } else {
            uploadStorageStatus.visibility = View.GONE
        }
    }

    private fun openAttachment(path: String) {
        if (path.startsWith("http")) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(path)))
        } else {
            Toast.makeText(requireContext(), "Attachment path: $path", Toast.LENGTH_LONG).show()
        }
    }

    /* ----------------------------
       Helper wrappers called by click handlers
       keeps UI state updates consistent
       ---------------------------- */
    private fun handleTsdAccept(
        bookingId: String,
        btnAccept: Button,
        btnReject: Button,
        prevAcceptText: String
    ) {
        Log.d(TAG, "tsd_bookings doc exists for $bookingId → forcing TSD flow")
        receiveTsdBooking(bookingId) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Marked as received", Toast.LENGTH_SHORT).show()
                try {
                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Log.e(TAG, "popBackStack failed", e)
                }
            } else {
                btnAccept.text = prevAcceptText
                btnAccept.isEnabled = true
                btnReject.isEnabled = true
            }
        }
    }

    private fun handleTransporterAccept(
        bookingId: String,
        btnAccept: Button,
        btnReject: Button,
        prevAcceptText: String
    ) {
        acceptBooking(bookingId) { success ->
            if (success) {
                try {
                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Log.e(TAG, "popBackStack failed", e)
                }
            } else {
                btnAccept.text = prevAcceptText
                btnAccept.isEnabled = true
                btnReject.isEnabled = true
            }
        }
    }

    private fun handleTsdReject(
        bookingId: String,
        btnAccept: Button,
        btnReject: Button,
        prevRejectText: String
    ) {
        treatTsdBooking(bookingId) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Marked as treated", Toast.LENGTH_SHORT).show()
                try {
                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Log.e(TAG, "popBackStack failed", e)
                }
            } else {
                btnReject.text = prevRejectText
                btnAccept.isEnabled = true
                btnReject.isEnabled = true
            }
        }
    }

    private fun handleTransporterReject(
        bookingId: String,
        btnAccept: Button,
        btnReject: Button,
        prevRejectText: String
    ) {
        rejectBooking(bookingId) { success ->
            if (success) {
                try {
                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Log.e(TAG, "popBackStack failed", e)
                }
            } else {
                btnReject.text = prevRejectText
                btnAccept.isEnabled = true
                btnReject.isEnabled = true
            }
        }
    }

    // Keep other existing methods you had (bindFromBundle, loadBookingFromFirestore, bindTransportDocToView, acceptBooking, rejectBooking, receiveTsdBooking, treatTsdBooking)
    // NOTE: if you moved those earlier in the file, ensure there are no duplicate method declarations.
}
