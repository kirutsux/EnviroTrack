package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentSpServiceRequestDetailsBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class SP_ServiceRequestDetails : Fragment() {

    private var _binding: FragmentSpServiceRequestDetailsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val TAG = "SRDetails"

    // Live listener for tsd booking when in TSD POV
    private var tsdListener: ListenerRegistration? = null

    // If you later add live listener for transport, keep a reference (optional)
    private var transportListener: ListenerRegistration? = null

    // runtime role (resolved from users/service_providers)
    private var currentRole: String = "transporter"

    // Track doc ids (from args)
    private var transportDocId: String? = null
    private var tsdDocId: String? = null
    private var currentBookingId: String? = null

    // Keep track of attachments
    private var currentCertificateUrl: String? = null
    private var currentPreviousRecordUrl: String? = null

    // Developer-provided fallback image file (local path)
    private val DEV_ATTACHMENT_URL = "/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png"

    // ActivityResult launchers for picking files
    private val pickFileForCertificate = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadFileAndSave(it, "certificate") }
    }
    private val pickFileForPreviousRecord = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadFileAndSave(it, "previousRecord") }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSpServiceRequestDetailsBinding.inflate(inflater, container, false)

        // Read args (support multiple possible keys)
        transportDocId = arguments?.getString("transportDocId")
            ?: arguments?.getString("transport_id")

// Only treat as TSD if explicitly passed via tsdDocId or requestId
        tsdDocId = arguments?.getString("tsdDocId")
            ?: arguments?.getString("requestId")

// Force transporter mode if transportDocId was provided
        if (!transportDocId.isNullOrBlank()) {
            tsdDocId = null
        }

        Log.d(TAG, "ARGS: transportDocId=$transportDocId tsdDocId=$tsdDocId extras=${arguments?.keySet()}")

        // Setup UI mode and load appropriate doc(s)
        setupModeAndLoad()

        // Wire buttons (robust: decide role at click time)
        setupButtonHandlers()

        return binding.root
    }

    private fun setupModeAndLoad() {
        // Prefer TSD mode when a tsdDocId is provided.
        val isTsdMode = !tsdDocId.isNullOrBlank()
        val isTransporterMode = !transportDocId.isNullOrBlank() && tsdDocId.isNullOrBlank()

        if (isTsdMode) {
            // TSD POV — hide transporter-only fields
            binding.txtWasteType.visibility = View.GONE
            binding.txtPackaging.visibility = View.GONE
            binding.txtSpecialInstructions.visibility = View.GONE
            ensureTreatmentInfoViews()

            // subscribe to the TSD doc for live updates
            subscribeToTsdRequest(tsdDocId!!)
        } else if (isTransporterMode) {
            binding.txtWasteType.visibility = View.VISIBLE
            binding.txtPackaging.visibility = View.VISIBLE
            binding.txtSpecialInstructions.visibility = View.VISIBLE

            // Use the unified loader: transport → TSD → bundle
            loadBookingFromTransportOrBundle(transportDocId!!, binding.root)
        } else {
            // Fallback: if you passed a generic "bookingId" try to load either collection;
            // otherwise bind from bundle/defaults.
            val generic = arguments?.getString("bookingId") ?: arguments?.getString("id")
            if (!generic.isNullOrBlank()) {
                loadBookingFromFirestore(generic)
            } else {
                bindFromBundle()
            }
        }
    }



    /**
     * Programmatically create treatment info views and insert in the card info area (if missing).
     * Re-used from your earlier implementation.
     */
    private var labelTreatmentInfoView: TextView? = null
    private var txtTreatmentInfoView: TextView? = null

    private fun ensureTreatmentInfoViews() {
        if (txtTreatmentInfoView != null && labelTreatmentInfoView != null) return

        // cardInfo -> inner linear layout -> info container (robust search)
        val cardInner = binding.cardInfo.getChildAt(0) as? LinearLayout ?: return
        var infoContainer: LinearLayout? = null
        for (i in 0 until cardInner.childCount) {
            val ch = cardInner.getChildAt(i)
            if (ch is LinearLayout && ch.orientation == LinearLayout.VERTICAL && ch.childCount >= 3) {
                infoContainer = ch
                break
            }
        }
        if (infoContainer == null) infoContainer = cardInner

        labelTreatmentInfoView = TextView(requireContext()).apply {
            text = "Treatment Info"
            setTextColor(resources.getColor(android.R.color.darker_gray, requireActivity().theme))
            textSize = 12f
            visibility = View.GONE
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dpToPx(12), 0, 0)
            layoutParams = lp
        }

        txtTreatmentInfoView = TextView(requireContext()).apply {
            text = ""
            setBackgroundResource(R.drawable.bg_field)
            setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
            setTextColor(resources.getColor(android.R.color.black, requireActivity().theme))
            visibility = View.GONE
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dpToPx(6), 0, 0)
            layoutParams = lp
        }

        // attempt to insert after packaging row
        var insertIndex = -1
        for (i in 0 until infoContainer.childCount) {
            val ch = infoContainer.getChildAt(i)
            if (ch.id == binding.txtPackaging.id) {
                insertIndex = i + 1
                break
            }
        }
        if (insertIndex == -1) {
            // fallback: append to end
            insertIndex = infoContainer.childCount
        }
        infoContainer.addView(labelTreatmentInfoView, insertIndex)
        infoContainer.addView(txtTreatmentInfoView, insertIndex + 1)
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    /**
     * Setup Accept / Reject button handlers. On click we resolve role (current user) and call appropriate handler.
     * Buttons use binding.* views.
     */
    private fun setupButtonHandlers() {
        val btnAccept = binding.btnAccept
        val btnReject = binding.btnReject

        btnAccept?.setOnClickListener {
            Log.d(TAG, "btnAccept CLICKED — resolving role...")
            val id = transportDocId ?: tsdDocId ?: currentBookingId
            if (id.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Missing booking id", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "btnAccept FAILED: bookingId is NULL")
                return@setOnClickListener
            }

            btnAccept.isEnabled = false
            btnReject.isEnabled = false
            val prevAcceptText = btnAccept.text?.toString() ?: "Accept"

            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Please sign in", Toast.LENGTH_SHORT).show()
                btnAccept.isEnabled = true
                btnReject.isEnabled = true
                return@setOnClickListener
            }

            // resolve role at click time (robust)
            fetchRoleForCurrentUser { role ->
                currentRole = role
                Log.d(TAG, "btnAccept USING ROLE = $role")
                if (role.contains("tsd")) {
                    btnAccept.text = "Receiving..."
                    handleTsdAccept(id, btnAccept, btnReject, prevAcceptText)
                } else {
                    btnAccept.text = "Accepting..."
                    handleTransporterAccept(id, btnAccept, btnReject, prevAcceptText)
                }
            }
        }

        btnReject?.setOnClickListener {
            Log.d(TAG, "btnReject CLICKED — resolving role...")
            val id = transportDocId ?: tsdDocId ?: currentBookingId
            if (id.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Missing booking id", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "btnReject FAILED: bookingId is NULL")
                return@setOnClickListener
            }

            btnAccept?.isEnabled = false
            btnReject.isEnabled = false
            val prevRejectText = btnReject.text?.toString() ?: "Reject"

            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Please sign in", Toast.LENGTH_SHORT).show()
                btnAccept?.isEnabled = true
                btnReject?.isEnabled = true
                return@setOnClickListener
            }

            fetchRoleForCurrentUser { role ->
                currentRole = role
                Log.d(TAG, "btnReject resolved role = $role")
                if (role.contains("tsd")) {
                    btnReject.text = "Treating..."
                    handleTsdReject(id, btnAccept, btnReject, prevRejectText)
                } else {
                    btnReject.text = "Rejecting..."
                    handleTransporterReject(id, btnAccept, btnReject, prevRejectText)
                }
            }
        }

        // Wire TSD upload buttons if present in layout (they may be null if not included)


        // Update status dialog for TSD (if button exists)

    }

    /* -------------------------
       Role resolver (reads users/{uid} or service_providers/{uid})
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

    /* ----------------------------
       Transporter: one-time load for transport_bookings/{transportId}
       (replaced with a snapshot listener so transport booking updates reflect immediately)
       ---------------------------- */


    /**
     * Subscribe to TSD booking document and populate treatmentInfo + other fields. (live listener)
     * (UNCHANGED - TSD handling kept exactly as before)
     */
    private fun subscribeToTsdRequest(tsdId: String) {
        binding.txtAttachments.text = "Loading attachments..."
        tsdListener?.remove()
        val ref = db.collection("tsd_bookings").document(tsdId)
        tsdListener = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                Toast.makeText(requireContext(), "Error loading TSD request: ${err.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            if (snap == null || !snap.exists()) {
                Toast.makeText(requireContext(), "TSD request not found", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            val m = snap.data ?: emptyMap<String, Any>()
            currentBookingId = snap.id

            // Basic fields
            binding.txtBookingId.text = (m["bookingId"] as? String) ?: snap.id
            binding.txtLocation.text = (m["location"] as? String) ?: ""

            val pref = (m["preferredDate"] as? String)
            binding.txtDateRequested.text = pref ?: ((m["dateCreated"] as? Timestamp)?.toDate()
                ?.let { DateFormat.format("MMM dd, yyyy • hh:mm a", it).toString() } ?: "")

            val facilityName = (m["facilityName"] as? String).orEmpty()
            val companyNameField = (m["companyName"] as? String).orEmpty()
            val contactNumber = (m["contactNumber"] as? String).orEmpty()
            val contactPersonName = (m["contactPerson"] as? String).orEmpty()

            val companyDisplay = when {
                facilityName.isNotBlank() -> facilityName
                companyNameField.isNotBlank() -> companyNameField
                else -> ""
            }
            binding.txtCompanyName.text = companyDisplay

            val contactDisplay = when {
                contactNumber.isNotBlank() && contactPersonName.isNotBlank() -> "$contactPersonName — $contactNumber"
                contactNumber.isNotBlank() -> contactNumber
                contactPersonName.isNotBlank() -> contactPersonName
                else -> ""
            }
            binding.txtContactPerson.text = contactDisplay

            // Hide transporter-only fields for TSD
            binding.txtWasteType.visibility = View.GONE
            binding.txtPackaging.visibility = View.GONE
            binding.txtSpecialInstructions.visibility = View.GONE

            binding.txtQuantity.text = when (val q = m["quantity"]) {
                is Number -> q.toString()
                is String -> q
                else -> ""
            }

            val total = (m["totalPayment"] as? Number)?.toDouble()
            val rate = (m["rate"] as? Number)?.toDouble()
            binding.txtAmount.text = when {
                total != null -> String.format("%,.2f", total)
                rate != null -> String.format("%,.2f", rate)
                else -> (m["amount"] as? String) ?: ""
            }

            // show treatment info
            ensureTreatmentInfoViews()
            val treatment = (m["treatmentInfo"] as? String) ?: (m["treatment"] as? String) ?: ""
            txtTreatmentInfoView?.text = treatment
            labelTreatmentInfoView?.visibility = View.VISIBLE
            txtTreatmentInfoView?.visibility = View.VISIBLE

            // attachments combine
            val attachments = mutableListOf<String>()
            (m["collectionProof"] as? List<*>)?.mapNotNull { it as? String }?.let { attachments.addAll(it) }
            (m["certificateUrl"] as? String)?.let { attachments.add(it) }
            (m["previousRecordUrl"] as? String)?.let { attachments.add(it) }
            (m["attachments"] as? List<*>)?.mapNotNull { it as? String }?.let { attachments.addAll(it) }
            (m["fileUrl"] as? String)?.let { attachments.add(it) }

            val firstAttachment = attachments.firstOrNull() ?: (m["previousRecordUrl"] as? String)
            ?: (m["certificateUrl"] as? String) ?: DEV_ATTACHMENT_URL
            setupAttachmentUI(firstAttachment)

            currentCertificateUrl = (m["certificateUrl"] as? String)?.ifEmpty { null }
            currentPreviousRecordUrl = (m["previousRecordUrl"] as? String)?.ifEmpty { null }

            // ------ STATUS: prefer bookingStatus, then status, else "Pending" ------
            val bookingStatus = when {
                ((m["bookingStatus"] as? String)?.trim()?.isNotEmpty() == true) -> (m["bookingStatus"] as String).trim()
                ((m["status"] as? String)?.trim()?.isNotEmpty() == true)        -> (m["status"] as String).trim()
                else -> "Pending"
            }

            // set the UI status pill so details reflect Firestore immediately
            binding.txtStatusPill.text = bookingStatus

            // Visibility decision based on bookingStatus
            val finalStates = setOf("confirmed", "treated", "rejected", "received", "completed")
            val nonActionable = finalStates.contains(bookingStatus.lowercase())

            activity?.runOnUiThread {
                if (nonActionable) {
                    binding.btnAccept.visibility = View.GONE
                    binding.btnReject.visibility = View.GONE
                    binding.btnAccept.isEnabled = false
                    binding.btnReject.isEnabled = false
                    Log.d(TAG, "TSD DECIDER: HIDING buttons because bookingStatus='$bookingStatus'")
                } else {
                    binding.btnAccept.visibility = View.VISIBLE
                    binding.btnReject.visibility = View.VISIBLE
                    binding.btnAccept.isEnabled = true
                    binding.btnReject.isEnabled = true
                    Log.d(TAG, "TSD DECIDER: SHOWING buttons because bookingStatus='$bookingStatus'")
                }
            }

            // clicking attachments opens first available
            val attText = binding.txtAttachments
            if (firstAttachment.isNotBlank()) {
                attText.setOnClickListener {
                    val urlToOpen = currentPreviousRecordUrl ?: currentCertificateUrl ?: firstAttachment
                    if (urlToOpen.isNotBlank()) openAttachment(urlToOpen)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            view?.let { recheckButtonsByStatusPill(it) }
        } catch (ex: Exception) {
            Log.w(TAG, "onResume recheck failed: ${ex.message}")
        }
    }

    private fun recheckButtonsByStatusPill(rootView: View) {
        val statusPill = binding.txtStatusPill.text?.toString()?.trim() ?: return
        val lower = statusPill.lowercase()
        val finalStates = setOf("confirmed", "treated", "rejected", "received", "completed")
        if (finalStates.contains(lower)) {
            binding.btnAccept.visibility = View.GONE
            binding.btnReject.visibility = View.GONE
            binding.btnAccept.isEnabled = false
            binding.btnReject.isEnabled = false
            Log.d(TAG, "onResume DECIDER: hid buttons based on statusPill='$statusPill'")
        } else {
            binding.btnAccept.visibility = View.VISIBLE
            binding.btnReject.visibility = View.VISIBLE
            binding.btnAccept.isEnabled = true
            binding.btnReject.isEnabled = true
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

        val updates = mutableMapOf<String, Any>(
            "status" to statusValue,
            "bookingStatus" to statusValue,
            "statusUpdatedAt" to FieldValue.serverTimestamp(),
            "statusUpdatedBy" to actorUid
        )
        lifecycleField?.let { updates[it] = FieldValue.serverTimestamp() }
        updates.putAll(extraFields)

        Log.d(TAG, "updateBookingStatusGeneric: collection=$collectionName id=$bookingId updates=$updates")

        // optimistic UI update: hide/disable buttons and set status pill while update is in flight
        try {
            activity?.runOnUiThread {
                binding.txtStatusPill.text = statusValue
                binding.btnAccept.visibility = View.GONE
                binding.btnReject.visibility = View.GONE
                binding.btnAccept.isEnabled = false
                binding.btnReject.isEnabled = false
            }
        } catch (ex: Exception) {
            Log.w(TAG, "optimistic UI update failed: ${ex.message}")
        }

        db.collection(collectionName)
            .document(bookingId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Firestore UPDATE SUCCESS [$collectionName/$bookingId] -> $statusValue")

                // notify list fragment(s) so rows refresh in-place without requiring a full re-query
                try {
                    val result = Bundle().apply {
                        putString("bookingId", bookingId)
                        putString("bookingStatus", statusValue)
                        putString("collection", collectionName)
                    }
                    parentFragmentManager.setFragmentResult("bookingStatusChanged", result)
                } catch (e: Exception) {
                    Log.w(TAG, "setFragmentResult failed: ${e.message}")
                }

                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore UPDATE FAILED [$collectionName/$bookingId]: ${e.message}", e)

                // rollback optimistic UI so user can retry
                try {
                    activity?.runOnUiThread {
                        binding.btnAccept.visibility = View.VISIBLE
                        binding.btnReject.visibility = View.VISIBLE
                        binding.btnAccept.isEnabled = true
                        binding.btnReject.isEnabled = true
                    }
                } catch (ex: Exception) { /* ignore */ }

                callback(false)
            }
    }


    // Transporter helpers
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

    // TSD helpers (unchanged)
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

    private fun treatTsdBooking(bookingId: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "treatTsdBooking() CALLED for bookingId=$bookingId")
        val uid = auth.currentUser?.uid ?: ""
        val extra = mapOf<String, Any>(
            "treatedBy" to uid,
            "treatmentInfo" to "Treated by $uid"
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
        val bookingId = currentBookingId
        if (bookingId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Booking ID missing", Toast.LENGTH_SHORT).show()
            return
        }

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
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val url = downloadUri.toString()
                Log.d(TAG, "Upload success, downloadUrl=$url")
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
                        loadTsdBookingFromFirestore(bookingId)
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
       (left intentionally as in your code)
       ---------------------------- */


    /* ----------------------------
       Helper: load TSD doc once (not listener) for refresh after upload/status change
       ---------------------------- */
    private fun loadTsdBookingFromFirestore(bookingId: String) {
        db.collection("tsd_bookings").document(bookingId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener
                // rebind by reusing subscribeToTsdRequest's logic: easiest is to just call subscribeToTsdRequest to refresh
                subscribeToTsdRequest(bookingId)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "loadTsdBookingFromFirestore failed: ${e.message}")
            }
    }

    /* ----------------------------
       Attachment UI binder (uses binding)
       ---------------------------- */
    private fun setupAttachmentUI(attachmentPath: String) {
        binding.txtAttachments.text = attachmentPath.substringAfterLast("/")
        binding.txtAttachments.tag = attachmentPath

        try {
            Glide.with(this)
                .load(attachmentPath)
                .error(Glide.with(this).load(DEV_ATTACHMENT_URL))
                .into(binding.imgCompanyLogo)
        } catch (ex: Exception) {
            Log.w(TAG, "Glide load failed, using fallback", ex)
            Glide.with(this).load(DEV_ATTACHMENT_URL).into(binding.imgCompanyLogo)
        }

        binding.txtAttachments.setOnClickListener {
            openAttachment(attachmentPath)
        }

        val uploadedTransport = attachmentPath.contains("transportPlan", ignoreCase = true) || attachmentPath.startsWith("http")
        if (uploadedTransport) {
            binding.txtUploadTransportPlanStatus.text = "Transport Plan: Uploaded"
            binding.txtUploadTransportPlanStatus.visibility = View.VISIBLE
        } else {
            binding.txtUploadTransportPlanStatus.visibility = View.GONE
        }

        if (attachmentPath.contains("storagePermit", ignoreCase = true)) {
            binding.txtUploadStoragePermitStatus.text = "Storage Permit: Uploaded"
            binding.txtUploadStoragePermitStatus.visibility = View.VISIBLE
        } else {
            binding.txtUploadStoragePermitStatus.visibility = View.GONE
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
       Handler wrappers called by button UI
       (TSD handlers left unchanged)
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
    /**
     * If the transport booking references a TSD booking, set that TSD booking to Confirmed as well.
     * Tries common link fields: "tsdBookingId", "tsdId", "bookingId", "requestId".
     */
    private fun propagateConfirmToTsdIfLinked(transportDocId: String) {
        val transportRef = db.collection("transport_bookings").document(transportDocId)
        transportRef.get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener
                val m = doc.data ?: return@addOnSuccessListener

                // Try common linking keys
                val possible = listOf("tsdBookingId", "tsdId", "bookingId", "requestId", "tsd_doc_id")
                val linkedId = possible.mapNotNull { key ->
                    when (val v = m[key]) {
                        is String -> v.trim().takeIf { it.isNotEmpty() }
                        else -> null
                    }
                }.firstOrNull()

                if (linkedId != null) {
                    // update tsd_bookings/{linkedId} to Confirmed (safe write: update both keys)
                    val uid = auth.currentUser?.uid ?: ""
                    val updates = mapOf<String, Any>(
                        "status" to "Confirmed",
                        "bookingStatus" to "Confirmed",
                        "statusUpdatedAt" to FieldValue.serverTimestamp(),
                        "statusUpdatedBy" to uid
                    )
                    db.collection("tsd_bookings").document(linkedId).update(updates)
                        .addOnSuccessListener {
                            Log.d(TAG, "Propagated Confirmed -> tsd_bookings/$linkedId")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to propagate to TSD ($linkedId): ${e.message}")
                        }
                } else {
                    Log.d(TAG, "No linked TSD id found on transport_bookings/$transportDocId")
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed reading transport doc to propagate: ${e.message}")
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
                // propagate to TSD if any linked tsd booking exists
                propagateConfirmToTsdIfLinked(bookingId)

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

    /* ----------------------------
       Additional transport binder used by the recovered code (keeps previous behavior)
       -- replaced GET with snapshot listener in loadTransporterRequest above --
       ---------------------------- */
    private fun loadBookingFromTransportOrBundle(bookingId: String, view: View) {

        val transportRef = db.collection("transport_bookings").document(bookingId)
        val tsdRef = db.collection("tsd_bookings").document(bookingId)

        // Try transport doc with a snapshot listener so UI stays in sync with Firestore
        transportListener?.remove()
        transportListener = transportRef.addSnapshotListener { docSnap, err ->
            if (err != null) {
                Log.e(TAG, "Error reading transport_bookings snapshot: ${err.message}")
                // fallback to trying TSD once
                try {
                    tsdRef.get()
                        .addOnSuccessListener { tsdDoc ->
                            if (tsdDoc.exists()) {
                                Log.d(TAG, "Loaded from TSD (after transport snapshot error): ${tsdDoc.id}")
                                subscribeToTsdRequest(tsdDoc.id)
                            } else {
                                Log.d(TAG, "No FS doc found → bundle (after transport snapshot error)")
                                currentBookingId = null
                                bindFromBundle()
                            }
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "Error reading tsd_bookings after transport snapshot error: ${it.message}")
                            currentBookingId = null
                            bindFromBundle()
                        }
                } catch (ex: Exception) {
                    Log.e(TAG, "Fallback TSd read failed: ${ex.message}")
                    currentBookingId = null
                    bindFromBundle()
                }
                return@addSnapshotListener
            }

            if (docSnap != null && docSnap.exists()) {
                val m = docSnap.data ?: emptyMap<String, Any>()
                Log.d(TAG, "Loaded from TRANSPORT (snapshot): ${docSnap.id} bookingStatus=${m["bookingStatus"] ?: m["status"]}")
                // IMPORTANT: record current booking id so buttons/uploads use the correct doc
                currentBookingId = docSnap.id
                transportDocId = docSnap.id

                bindTransportDocToView(m, docSnap.id)
            } else {
                // If transport doesn't exist, try TSD once (one-time get)
                tsdRef.get()
                    .addOnSuccessListener { tsdDoc ->
                        if (tsdDoc.exists()) {
                            Log.d(TAG, "Loaded from TSD: ${tsdDoc.id}")
                            subscribeToTsdRequest(tsdDoc.id)   // LIVE updates (TSD) restored
                        } else {
                            // fallback to bundle
                            Log.d(TAG, "No FS doc found → bundle")
                            currentBookingId = null
                            bindFromBundle()
                        }
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "Error reading tsd_bookings: ${it.message}")
                        currentBookingId = null
                        bindFromBundle()
                    }
            }
        }
    }

    private fun bindTransportDocToView(m: Map<String, Any>, docId: String) {
        // ensure callers have the correct active booking id recorded
        currentBookingId = docId
        transportDocId = docId

        fun s(key: String, alt: String = ""): String {
            val v = (m[key] as? String)?.trim()
            return if (!v.isNullOrEmpty()) v else alt
        }

        val companyName = s("serviceProviderCompany", s("companyName", "Unknown"))
        val wasteType = s("wasteType")
        val serviceType = if (wasteType.isNotBlank()) "Transport - $wasteType" else "Transport Booking"
        val origin = s("origin", s("pickupLocation", "N/A"))

        val dateRequested = (m["bookingDate"] as? Timestamp)?.toDate()?.let {
            DateFormat.format("MMM dd, yyyy hh:mm a", it).toString()
        } ?: (m["dateBooked"] as? Timestamp)?.toDate()?.let {
            DateFormat.format("MMM dd, yyyy hh:mm a", it).toString()
        } ?: "N/A"

        val providerContact = s("providerContact", s("contactNumber", "N/A"))
        val providerName = s("serviceProviderName", s("name", ""))

        // --- Use the same robust status resolution as TSD ---
        val bookingStatus = when {
            ((m["bookingStatus"] as? String)?.trim()?.isNotEmpty() == true) -> (m["bookingStatus"] as String).trim()
            ((m["status"] as? String)?.trim()?.isNotEmpty() == true)        -> (m["status"] as String).trim()
            else -> "Pending"
        }

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

        binding.txtCompanyName.text = companyName
        binding.txtServiceType.text = serviceType
        binding.txtLocation.text = origin
        binding.txtDateRequested.text = dateRequested
        binding.txtContactPerson.text = if (providerName.isNotEmpty()) "$providerName — $providerContact" else providerContact

        // set the pill text from resolved bookingStatus
        binding.txtStatusPill.text = bookingStatus
        binding.txtNotes.text = notes

        binding.txtWasteType.text = wasteType.ifEmpty { "-" }
        binding.txtQuantity.text = quantity.ifEmpty { "-" }
        binding.txtPackaging.text = packaging.ifEmpty { "-" }

        // --- Payment status: if paymentStatus == "Paid" show "Paid" in amount, else show numeric amount ---
        // Prefer explicit "amount" field (number or string). Fallback to totalPayment / rate.
// Only use paymentStatus as a last resort.
        val amountField = m["amount"]

        val paymentStatusLower = (m["paymentStatus"] as? String)
            ?.trim()
            ?.lowercase()

        val amountText: String? = when (amountField) {
            is Number -> "₱ ${"%,.2f".format(amountField.toDouble())}"
            is String -> {
                val s = amountField.trim()
                if (s.isEmpty()) null
                else {
                    // auto-add peso sign if missing
                    if (s.contains("₱") || s.contains("$") || s.matches(Regex(".*[A-Za-z].*"))) s
                    else "₱ $s"
                }
            }
            else -> null
        }

        if (!amountText.isNullOrEmpty()) {
            // 👍 We found a real numeric/string amount, show it exactly
            binding.txtAmount.text = amountText
        } else {
            // fallback numeric fields
            val total = (m["totalPayment"] as? Number)?.toDouble()
            val rate = (m["rate"] as? Number)?.toDouble()

            when {
                total != null -> binding.txtAmount.text =
                    "₱ ${"%,.2f".format(total)}"

                rate != null -> binding.txtAmount.text =
                    "₱ ${"%,.2f".format(rate)}"

                // last fallback → if Paid but no amount, show "Paid"
                paymentStatusLower == "paid" ->
                    binding.txtAmount.text = "Paid"

                else ->
                    binding.txtAmount.text = ""
            }
        }


        val firstAttachment = transportPlanUrl ?: storagePermitUrl ?: DEV_ATTACHMENT_URL
        setupAttachmentUI(firstAttachment)

        // same final-states logic used in TSD listener
        // Transporter final-state logic (same behavior as TSD)
        val lowerStatus = bookingStatus.lowercase()
        val finalStates = setOf("confirmed", "rejected", "completed", "received", "treated")

        if (finalStates.contains(lowerStatus)) {
            binding.btnAccept.visibility = View.GONE
            binding.btnReject.visibility = View.GONE
            binding.btnAccept.isEnabled = false
            binding.btnReject.isEnabled = false
            Log.d(TAG, "TRANSPORT: Hiding buttons because status='$bookingStatus'")
        } else {
            binding.btnAccept.visibility = View.VISIBLE
            binding.btnReject.visibility = View.VISIBLE
            binding.btnAccept.isEnabled = true
            binding.btnReject.isEnabled = true
            Log.d(TAG, "TRANSPORT: Showing buttons because status='$bookingStatus'")
        }
    }



    // ---------- REPLACE existing bindFromBundle() with this ----------
    private fun bindFromBundle() {
        // clear any previous ids so actions are not applied to a stale doc
        currentBookingId = null
        transportDocId = null
        // prefer explicit bundle args
        var companyName = arguments?.getString("companyName") ?: ""
        val serviceType = arguments?.getString("serviceTitle") ?: "Transport Booking"
        val location = arguments?.getString("origin") ?: "N/A"
        val dateRequested = arguments?.getString("dateRequested") ?: "N/A"
        val providerContact = arguments?.getString("providerContact") ?: "N/A"
        val providerName = arguments?.getString("providerName") ?: ""
        val bookingStatus = arguments?.getString("bookingStatus") ?: "Pending"
        val notes = arguments?.getString("notes") ?: "No additional notes"
        val attachment = arguments?.getString("attachment") ?: DEV_ATTACHMENT_URL

        // new: read wasteType, quantity, packaging from args if present
        val wasteTypeArg = arguments?.getString("wasteType") ?: ""
        val quantityArg = arguments?.getString("quantity") ?: ""
        val packagingArg = arguments?.getString("packaging") ?: ""

        // If companyName absent, attempt a *read-only* lookup by providerName (non-destructive)
        if (companyName.isBlank() && providerName.isNotBlank()) {
            // run a single query and quietly set companyName if found
            FirebaseFirestore.getInstance()
                .collection("service_providers")
                .whereEqualTo("serviceProviderName", providerName)
                .limit(1)
                .get()
                .addOnSuccessListener { qs ->
                    val found = qs.documents.firstOrNull()?.getString("companyName").orEmpty()
                    if (found.isNotBlank()) {
                        companyName = found
                        try { binding.txtCompanyName.text = companyName } catch (_: Exception) {}
                    }
                }
                .addOnFailureListener { /* ignore quietly */ }
        }

        // Set UI values (bundle-first)
        binding.txtCompanyName.text = if (companyName.isNotBlank()) companyName else "Unknown"
        binding.txtServiceType.text = serviceType
        binding.txtLocation.text = location
        binding.txtDateRequested.text = dateRequested
        binding.txtContactPerson.text = if (providerName.isNotEmpty()) "$providerName — $providerContact" else providerContact
        binding.txtStatusPill.text = bookingStatus
        binding.txtNotes.text = notes

        // set attachments
        setupAttachmentUI(attachment)

        // populate wasteType/quantity/packaging from args (fallback to placeholders)
        binding.txtWasteType.text = if (wasteTypeArg.isNotBlank()) wasteTypeArg else "-"
        binding.txtQuantity.text = if (quantityArg.isNotBlank()) quantityArg else "-"
        binding.txtPackaging.text = if (packagingArg.isNotBlank()) packagingArg else "-"
    }


    /* ----------------------------
       Utility: load booking by id (public) - tries tsd then transport
       ---------------------------- */
    private fun loadBookingFromFirestore(bookingId: String) {
        // prefer transport_bookings then tsd_bookings
        // Use snapshot listener for transport to catch updates, fallback to TSD if not found
        val transportDocRef = db.collection("transport_bookings").document(bookingId)

        // remove existing transport listener if any
        transportListener?.remove()
        transportListener = transportDocRef.addSnapshotListener { docSnap, err ->
            if (err != null) {
                Log.w(TAG, "transport snapshot error: ${err.message}")
                // fallback to one-time TSd read
                db.collection("tsd_bookings").document(bookingId)
                    .get()
                    .addOnSuccessListener { tsdDoc ->
                        if (tsdDoc.exists()) {
                            subscribeToTsdRequest(tsdDoc.id)
                        } else {
                            currentBookingId = null
                            bindFromBundle()
                        }
                    }
                    .addOnFailureListener {
                        currentBookingId = null
                        bindFromBundle()
                    }
                return@addSnapshotListener
            }

            if (docSnap != null && docSnap.exists()) {
                // record id so other operations (accept/reject/uploads) have the correct target
                currentBookingId = docSnap.id
                transportDocId = docSnap.id

                bindTransportDocToView(docSnap.data ?: emptyMap(), docSnap.id)
                return@addSnapshotListener
            } else {
                // try tsd once
                db.collection("tsd_bookings").document(bookingId)
                    .get()
                    .addOnSuccessListener { tsdDoc ->
                        if (tsdDoc.exists()) {
                            subscribeToTsdRequest(tsdDoc.id)
                        } else {
                            currentBookingId = null
                            bindFromBundle()
                        }
                    }
                    .addOnFailureListener {
                        currentBookingId = null
                        bindFromBundle()
                    }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tsdListener?.remove()
        transportListener?.remove()
        _binding = null
    }
}
