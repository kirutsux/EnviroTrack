package com.ecocp.capstoneenvirotrack.view.serviceprovider

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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class SP_ServiceRequestDetails : Fragment() {

    // Developer-provided fallback image file (local path)
    private val DEV_ATTACHMENT_URL = "/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png"

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "SRDetails"

    // runtime role (determined at start)
    private var currentRole: String = "transporter" // default to transporter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_sp_service_request_details, container, false)

        // bookingId passed by adapter (preferred)
        val bookingId = arguments?.getString("bookingId") ?: arguments?.getString("id")

        // detect role first, then load booking accordingly
        detectRoleAndLoadBooking(bookingId, view)

        // Buttons
        val btnAccept = view.findViewById<Button>(R.id.btnAccept)
        val btnReject = view.findViewById<Button>(R.id.btnReject)

        // set click listeners (actions will dispatch based on currentRole)
        btnAccept.setOnClickListener {
            Log.d(TAG, "btnAccept CLICKED (role=$currentRole)")

            val id = bookingId ?: arguments?.getString("bookingId") ?: arguments?.getString("id")
            Log.d(TAG, "btnAccept bookingId = $id")

            if (id.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Missing booking id", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "btnAccept FAILED: bookingId is NULL")
                return@setOnClickListener
            }

            // Visual state
            btnAccept.isEnabled = false
            btnReject.isEnabled = false
            val prevAcceptText = btnAccept.text
            btnAccept.text = when (currentRole) {
                "tsd", "tsdfacility" -> "Receiving..."
                else -> "Accepting..."
            }

            if (currentRole == "tsd" || currentRole == "tsdfacility") {
                // TSD: mark as Received (tsd_bookings)
                receiveTsdBooking(id) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Marked as received", Toast.LENGTH_SHORT).show()
                        try { findNavController().popBackStack() } catch (e: Exception) { Log.e(TAG, "popBackStack failed", e) }
                    } else {
                        btnAccept.text = prevAcceptText
                        btnAccept.isEnabled = true
                        btnReject.isEnabled = true
                    }
                }
            } else {
                // Transporter: accept booking (transport_bookings)
                acceptBooking(id) { success ->
                    if (success) {
                        try { findNavController().popBackStack() } catch (e: Exception) { Log.e(TAG, "popBackStack failed", e) }
                    } else {
                        btnAccept.text = prevAcceptText
                        btnAccept.isEnabled = true
                        btnReject.isEnabled = true
                    }
                }
            }
        }

        // Reject / Treat button
        btnReject.setOnClickListener {
            Log.d(TAG, "btnReject CLICKED (role=$currentRole)")

            val id = bookingId ?: arguments?.getString("bookingId") ?: arguments?.getString("id")
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
            btnReject.text = when (currentRole) {
                "tsd", "tsdfacility" -> "Treating..."
                else -> "Rejecting..."
            }

            if (currentRole == "tsd" || currentRole == "tsdfacility") {
                // TSD: treat booking (tsd_bookings)
                treatTsdBooking(id) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Marked as treated", Toast.LENGTH_SHORT).show()
                        try { findNavController().popBackStack() } catch (e: Exception) { Log.e(TAG, "popBackStack failed", e) }
                    } else {
                        btnReject.text = prevRejectText
                        btnAccept.isEnabled = true
                        btnReject.isEnabled = true
                    }
                }
            } else {
                // Transporter: reject booking (transport_bookings)
                rejectBooking(id) { success ->
                    if (success) {
                        try { findNavController().popBackStack() } catch (e: Exception) { Log.e(TAG, "popBackStack failed", e) }
                    } else {
                        btnReject.text = prevRejectText
                        btnAccept.isEnabled = true
                        btnReject.isEnabled = true
                    }
                }
            }
        }

        return view
    }

    /**
     * Detect user role from users/{uid}.role (fallback to transporter)
     * Then load booking from the appropriate collection.
     */
    private fun detectRoleAndLoadBooking(bookingId: String?, view: View) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            // no user signed in — default behaviour: bind bundle or load transport doc if id present
            Log.w(TAG, "User not signed in, defaulting to transporter behaviour")
            bookingId?.let { loadBookingFromTransportOrBundle(it, view) } ?: bindFromBundle(view)
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val role = userDoc.getString("role")?.trim()?.lowercase()
                currentRole = role ?: "transporter"
                Log.d(TAG, "Detected role = $currentRole")

                if (currentRole == "tsd" || currentRole == "tsdfacility" || currentRole == "tsd_facility") {
                    // adjust button labels for clarity
                    view.findViewById<Button>(R.id.btnAccept).text = "Receive"
                    view.findViewById<Button>(R.id.btnReject).text = "Treat"

                    // load tsd booking doc
                    if (!bookingId.isNullOrBlank()) {
                        loadTsdBookingFromFirestore(bookingId, view)
                    } else {
                        bindFromBundle(view)
                    }
                } else {
                    // transporter path
                    view.findViewById<Button>(R.id.btnAccept).text = "Accept"
                    view.findViewById<Button>(R.id.btnReject).text = "Reject"

                    if (!bookingId.isNullOrBlank()) {
                        loadBookingFromFirestore(bookingId, view)
                    } else {
                        bindFromBundle(view)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to read users/{uid} role: ${e.message}. Falling back to transporter")
                currentRole = "transporter"
                if (!bookingId.isNullOrBlank()) {
                    loadBookingFromFirestore(bookingId, view)
                } else {
                    bindFromBundle(view)
                }
            }
    }

    // helper that loads transport booking or falls back to bundle bindings
    private fun loadBookingFromTransportOrBundle(bookingId: String, view: View) {
        db.collection("transport_bookings").document(bookingId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    bindFromBundle(view)
                    return@addOnSuccessListener
                }
                bindTransportDocToView(doc.data ?: emptyMap(), doc.id, view)
            }
            .addOnFailureListener {
                bindFromBundle(view)
            }
    }

    private fun bindFromBundle(view: View) {
        val companyName     = arguments?.getString("companyName")     ?: "Unknown"
        val serviceType     = arguments?.getString("serviceTitle")    ?: "Transport Booking"
        val location        = arguments?.getString("origin")          ?: "N/A"
        val dateRequested   = arguments?.getString("dateRequested")   ?: "N/A"
        val providerContact = arguments?.getString("providerContact") ?: "N/A"
        val providerName    = arguments?.getString("providerName")    ?: ""
        val bookingStatus          = arguments?.getString("bookingStatus")          ?: "Pending"
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
    // TRANSPORT booking binder (keeps your existing logic)
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
        // NEW: HIDE ACCEPT/REJECT IF CONFIRMED OR REJECTED
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
    // TSD booking binder
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
                Log.d(TAG, "tsd docId=${doc.id} data=$m")

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
                val bookingStatus = s("status", "Pending")
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

                val firstAttachment = if (previousRecordUrl.isNotBlank()) previousRecordUrl else DEV_ATTACHMENT_URL
                setupAttachmentUI(view, firstAttachment)

                // Button visibility for TSD:
                val btnAccept = view.findViewById<Button>(R.id.btnAccept)
                val btnReject = view.findViewById<Button>(R.id.btnReject)

                if (bookingStatus.equals("Received", true) || bookingStatus.equals("Treated", true)) {
                    btnAccept.visibility = View.GONE
                    btnReject.visibility = View.GONE
                } else {
                    // show both; labels were already set by role detection
                    btnAccept.visibility = View.VISIBLE
                    btnReject.visibility = View.VISIBLE
                }

                // If certificate exists, let user open it by tapping attachment
                if (certificateUrl.isNotBlank()) {
                    view.findViewById<TextView>(R.id.txtAttachments).setOnClickListener {
                        openAttachment(certificateUrl)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed loading tsd booking", e)
                Toast.makeText(requireContext(), "Failed to load TSD booking: ${e.message}", Toast.LENGTH_LONG).show()
                bindFromBundle(view)
            }
    }

    // ----------------------------
    // Helper: accept booking and call callback(success)  (TRANSPORTER)
    // ----------------------------
    private fun acceptBooking(bookingId: String, callback: (Boolean) -> Unit) {

        Log.d(TAG, "acceptBooking() CALLED for bookingId=$bookingId")

        val uid = auth.currentUser?.uid
        Log.d(TAG, "Current provider UID = $uid")

        if (uid.isNullOrBlank()) {
            Log.e(TAG, "acceptBooking FAILED: uid is NULL")
            Toast.makeText(requireContext(), "Please sign in", Toast.LENGTH_SHORT).show()
            callback(false)
            return
        }

        val updates = mapOf<String, Any>(
            "bookingStatus" to "Confirmed",
            "providerId" to uid,
            "assignedAt" to FieldValue.serverTimestamp()
        )

        Log.d(TAG, "Updating Firestore with: $updates")

        db.collection("transport_bookings")
            .document(bookingId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Firestore UPDATE SUCCESS for bookingId=$bookingId")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore UPDATE FAILED: ${e.message}", e)
                Toast.makeText(requireContext(), "Accept failed: ${e.message}", Toast.LENGTH_LONG).show()
                callback(false)
            }
    }

    // ----------------------------
    // Helper: reject booking and call callback(success)  (TRANSPORTER)
    // ----------------------------
    private fun rejectBooking(bookingId: String, callback: (Boolean) -> Unit) {

        Log.d(TAG, "rejectBooking() CALLED for bookingId=$bookingId")

        val uid = auth.currentUser?.uid
        Log.d(TAG, "Current provider UID = $uid")

        if (uid.isNullOrBlank()) {
            Log.e(TAG, "rejectBooking FAILED: uid is NULL")
            Toast.makeText(requireContext(), "Please sign in", Toast.LENGTH_SHORT).show()
            callback(false)
            return
        }

        val updates = mapOf<String, Any>(
            "bookingStatus" to "Rejected",
            "rejectedAt" to FieldValue.serverTimestamp(),
            "rejectedBy" to uid
        )

        Log.d(TAG, "Updating Firestore with: $updates")

        db.collection("transport_bookings")
            .document(bookingId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Firestore UPDATE SUCCESS for bookingId=$bookingId")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore UPDATE FAILED: ${e.message}", e)
                Toast.makeText(requireContext(), "Reject failed: ${e.message}", Toast.LENGTH_LONG).show()
                callback(false)
            }
    }

    // ----------------------------
    // Helper: receive booking and call callback(success)  (TSD)
    // ----------------------------
    private fun receiveTsdBooking(bookingId: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "receiveTsdBooking() CALLED for bookingId=$bookingId")
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            Log.e(TAG, "receiveTsdBooking FAILED: uid is NULL")
            Toast.makeText(requireContext(), "Please sign in", Toast.LENGTH_SHORT).show()
            callback(false)
            return
        }

        val updates = mapOf<String, Any>(
            "status" to "Received",
            "receivedAt" to FieldValue.serverTimestamp(),
            "receivedBy" to uid
        )

        Log.d(TAG, "Updating TSD Firestore with: $updates")

        db.collection("tsd_bookings")
            .document(bookingId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "TSD Firestore UPDATE SUCCESS for bookingId=$bookingId")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "TSD Firestore UPDATE FAILED: ${e.message}", e)
                Toast.makeText(requireContext(), "Receive failed: ${e.message}", Toast.LENGTH_LONG).show()
                callback(false)
            }
    }

    // ----------------------------
    // Helper: treat booking and call callback(success)  (TSD)
    // ----------------------------
    private fun treatTsdBooking(bookingId: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "treatTsdBooking() CALLED for bookingId=$bookingId")
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            Log.e(TAG, "treatTsdBooking FAILED: uid is NULL")
            Toast.makeText(requireContext(), "Please sign in", Toast.LENGTH_SHORT).show()
            callback(false)
            return
        }

        val updates = mapOf<String, Any>(
            "status" to "Treated",
            "treatedAt" to FieldValue.serverTimestamp(),
            "treatedBy" to uid,
            // store some basic trace — in real app you'd store detailed treatmentInfo
            "treatmentInfo" to "Treated via mobile app by $uid"
        )

        Log.d(TAG, "Updating TSD Firestore with: $updates")

        db.collection("tsd_bookings")
            .document(bookingId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "TSD Firestore UPDATE SUCCESS for bookingId=$bookingId")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "TSD Firestore UPDATE FAILED: ${e.message}", e)
                Toast.makeText(requireContext(), "Treat failed: ${e.message}", Toast.LENGTH_LONG).show()
                callback(false)
            }
    }

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
}
