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

    // Developer-provided fallback image file (local path from your uploads)
    // This path will be transformed by your environment if needed:
    // /mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png
    private val DEV_ATTACHMENT_URL = "/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png"

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "SRDetails"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_sp_service_request_details, container, false)

        // bookingId passed by adapter (preferred)
        val bookingId = arguments?.getString("bookingId") ?: arguments?.getString("id")

        if (!bookingId.isNullOrBlank()) {
            loadBookingFromFirestore(bookingId, view)
        } else {
            bindFromBundle(view)
        }

        // Buttons
        val btnAccept = view.findViewById<Button>(R.id.btnAccept)
        val btnReject = view.findViewById<Button>(R.id.btnReject)

        // Accept -> call helper function
        // ACCEPT BUTTON
        btnAccept.setOnClickListener {
            Log.d(TAG, "btnAccept CLICKED")

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
            btnAccept.text = "Accepting..."

            Log.d(TAG, "Calling acceptBooking($id)")
            acceptBooking(id) { success ->
                Log.d(TAG, "acceptBooking callback: success=$success")

                if (success) {
                    Log.d(TAG, "acceptBooking SUCCESS → popping backstack...")
                    try {
                        findNavController().popBackStack()
                    } catch (e: Exception) {
                        Log.e(TAG, "popBackStack failed after accept", e)
                        btnAccept.text = prevAcceptText
                        btnAccept.isEnabled = true
                        btnReject.isEnabled = true
                    }
                } else {
                    Log.e(TAG, "acceptBooking FAILED → restoring UI")
                    btnAccept.text = prevAcceptText
                    btnAccept.isEnabled = true
                    btnReject.isEnabled = true
                }
            }
        }


        // Reject -> call helper function
        // REJECT BUTTON
        btnReject.setOnClickListener {
            Log.d(TAG, "btnReject CLICKED")

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
            btnReject.text = "Rejecting..."

            Log.d(TAG, "Calling rejectBooking($id)")
            rejectBooking(id) { success ->
                Log.d(TAG, "rejectBooking callback: success=$success")

                if (success) {
                    Log.d(TAG, "rejectBooking SUCCESS → popping backstack...")
                    try {
                        findNavController().popBackStack()
                    } catch (e: Exception) {
                        Log.e(TAG, "popBackStack failed after reject", e)
                        btnReject.text = prevRejectText
                        btnAccept.isEnabled = true
                        btnReject.isEnabled = true
                    }
                } else {
                    Log.e(TAG, "rejectBooking FAILED → restoring UI")
                    btnReject.text = prevRejectText
                    btnAccept.isEnabled = true
                    btnReject.isEnabled = true
                }
            }
        }


        return view
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
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed loading booking", e)
                Toast.makeText(requireContext(), "Failed to load booking: ${e.message}", Toast.LENGTH_LONG).show()
                bindFromBundle(view)
            }
    }


    // Helper: accept booking and call callback(success)
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



    // Helper: reject booking and call callback(success)
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
