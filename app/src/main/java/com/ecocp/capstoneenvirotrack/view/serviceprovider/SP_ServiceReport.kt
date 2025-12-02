package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentSpServiceReportBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class SP_ServiceReport : Fragment() {

    private var _binding: FragmentSpServiceReportBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private var tsdListener: ListenerRegistration? = null

    // ids passed as args
    private var transportDocId: String? = null
    private var tsdDocId: String? = null

    // Attachment handling
    private var currentAttachmentUrl: String? = null
    private var currentFileName: String? = null
    private val DEV_FALLBACK = "/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpServiceReportBinding.inflate(inflater, container, false)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnBackToList.setOnClickListener { findNavController().popBackStack() }
        binding.btnDownload.setOnClickListener { onDownloadClicked() }

        // Read args
        transportDocId = arguments?.getString("transportDocId") // transporter opens with this
        tsdDocId = arguments?.getString("tsdDocId")
            ?: arguments?.getString("requestId")
                    ?: arguments?.getString("bookingId")

        // Decide mode and load appropriate data
        setupModeAndLoad()

        return binding.root
    }

    private fun setupModeAndLoad() {

        val explicitTransporter = !transportDocId.isNullOrBlank()

        if (explicitTransporter) {
            showTransporterUIAndLoad(transportDocId!!)
            return
        }

        // ↓ If only tsdDocId/requestId is passed, detect which collection it belongs to
        val candidateId = tsdDocId
        if (candidateId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "No booking id provided", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if this ID exists in transport_bookings
        db.collection("transport_bookings").document(candidateId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // This is actually a TRANSPORT booking → Transporter view!
                    showTransporterUIAndLoad(candidateId)
                } else {
                    // Not in transport → treat it as a TSD booking
                    showTsdUIAndSubscribe(candidateId)
                }
            }
            .addOnFailureListener {
                // If error, default to TSD to avoid breaking anything
                showTsdUIAndSubscribe(candidateId)
            }
    }

    private fun showTransporterUIAndLoad(id: String) {
        binding.tsdSection.visibility = View.GONE

        binding.labelCompletionDate.visibility = View.VISIBLE
        binding.txtCompletionDate.visibility = View.VISIBLE
        binding.wasteTypeRow.visibility = View.VISIBLE

        binding.txtProviderLabel.text = "Service Provider"

        loadTransporterDoc(id)
    }

    private fun showTsdUIAndSubscribe(id: String) {
        binding.tsdSection.visibility = View.VISIBLE

        binding.labelCompletionDate.visibility = View.GONE
        binding.txtCompletionDate.visibility = View.GONE
        binding.wasteTypeRow.visibility = View.GONE

        binding.txtProviderLabel.text = "Facility Name"

        subscribeToTsd(id)
    }



    private fun loadTransporterDoc(transportId: String) {
        binding.progressDownload.visibility = View.VISIBLE
        db.collection("transport_bookings").document(transportId).get()
            .addOnSuccessListener { doc ->
                binding.progressDownload.visibility = View.GONE
                val m = doc.data ?: emptyMap<String, Any>()

                // Map relevant transporter fields to UI (keep layout same)
                binding.txtBookingId.text = (m["bookingId"] as? String) ?: doc.id
                binding.txtReportRef.text = "Ref: ${binding.txtBookingId.text}"

                // provider/company
                val providerName = (m["serviceProviderCompany"] as? String)?.let { comp ->
                    val pname = (m["serviceProviderName"] as? String).orEmpty()
                    if (pname.isBlank()) comp else "$comp - $pname"
                } ?: (m["serviceProviderName"] as? String) ?: ""
                binding.txtProviderName.text = providerName
                binding.txtProviderContact.text = (m["providerContact"] as? String) ?: (m["contactNumber"] as? String) ?: ""

                // Booking date / Completion date (transport UI expects completion)
                val bookingTs = (m["bookingDate"] as? Timestamp)
                    ?: (m["dateBooked"] as? Timestamp)
                    ?: (m["dateCreated"] as? Timestamp)
                binding.txtBookingDate.text = bookingTs?.toDate()?.let {
                    DateFormat.format("MMM dd, yyyy • hh:mm a", it).toString()
                } ?: ""

                val completedTs = (m["completedAt"] as? Timestamp)
                    ?: (m["receivedAt"] as? Timestamp)
                    ?: (m["confirmedAt"] as? Timestamp)
                binding.txtCompletionDate.text = completedTs?.toDate()?.let {
                    DateFormat.format("MMM dd, yyyy • hh:mm a", it).toString()
                } ?: ""

                // Waste type (transport uses this)
                binding.txtWasteType.text = (m["wasteType"] as? String) ?: (m["waste"] as? String) ?: ""

                // quantity / remarks
                binding.txtQuantity.text = when (val q = m["quantity"]) {
                    is Number -> q.toString()
                    is String -> q
                    else -> ""
                }
                binding.txtRemarks.text = (m["specialInstructions"] as? String)
                    ?: (m["notes"] as? String)
                            ?: (m["remarks"] as? String)
                            ?: binding.txtRemarks.text

                // ---------- PAYMENT: show "Paid" if paymentStatus == "Paid", else show amount ----------
                val paymentStatus = (m["paymentStatus"] as? String)?.trim()
                val totalPayment = when (val t = m["totalPayment"]) {
                    is Number -> t.toDouble()
                    is String -> t.toDoubleOrNull()
                    else -> null
                }
                val amountFieldNumber = when (val a = m["amount"]) {
                    is Number -> a.toDouble()
                    is String -> a.toDoubleOrNull()
                    else -> null
                }
                val rate = when (val r = m["rate"]) {
                    is Number -> r.toDouble()
                    is String -> r.toDoubleOrNull()
                    else -> null
                }

                val formattedAmount = when {
                    totalPayment != null -> "₱${"%,.2f".format(totalPayment)}"
                    amountFieldNumber != null -> "₱${"%,.2f".format(amountFieldNumber)}"
                    rate != null -> "₱${"%,.2f".format(rate)}"
                    (m["amount"] as? String).isNullOrBlank().not() -> (m["amount"] as? String) ?: "₱0"
                    else -> "₱0"
                }

                binding.txtPayment.text = if (!paymentStatus.isNullOrBlank() && paymentStatus.equals("paid", ignoreCase = true)) {
                    "Paid"
                } else {
                    formattedAmount
                }

                // Attachments (transport fields)
                val attachments = mutableListOf<String>()
                (m["collectionProof"] as? List<*>)?.mapNotNull { it as? String }?.let { attachments.addAll(it) }
                (m["finalReportUrl"] as? String)?.let { attachments.add(it) }
                (m["certificateUrl"] as? String)?.let { attachments.add(it) }
                (m["attachments"] as? List<*>)?.mapNotNull { it as? String }?.let { attachments.addAll(it) }
                (m["fileUrl"] as? String)?.let { attachments.add(it) }

                if (attachments.isEmpty()) attachments.add(DEV_FALLBACK)

                currentAttachmentUrl = attachments.firstOrNull()
                currentFileName = currentAttachmentUrl?.substringAfterLast("/")?.substringBefore("?") ?: "attachment"
                binding.txtFileName.text = currentFileName
                binding.txtFileSize.text = "" // optional

                if (isImageUrl(currentAttachmentUrl)) {
                    Glide.with(this).load(currentAttachmentUrl).into(binding.imgFileIcon)
                } else {
                    binding.imgFileIcon.setImageResource(R.drawable.ic_pdf)
                }

                // Show facilityName/location in provider/company fields if present
                // Prefer transporter/company fields for the header (transport POV).
                val companyHeader = (m["serviceProviderCompany"] as? String)?.takeIf { it.isNotBlank() }
                    ?: (m["companyName"] as? String)?.takeIf { it.isNotBlank() }
                    ?: (m["serviceProviderName"] as? String)?.takeIf { it.isNotBlank() }
                    // fallback to facilityName only if none of the transporter fields are present
                    ?: (m["facilityName"] as? String)?.takeIf { it.isNotBlank() }
                    ?: "Unknown"

                binding.txtCompanyName.text = companyHeader
                binding.txtCompanyAddress.text = (m["location"] as? String) ?: binding.txtCompanyAddress.text

// Also ensure the small status pill reflects transport status when in transporter view
                binding.txtSmallStatus.text = (m["bookingStatus"] as? String) ?: (m["status"] as? String) ?: binding.txtSmallStatus.text

            }
            .addOnFailureListener { e ->
                binding.progressDownload.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed loading transport report: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun subscribeToTsd(tsdId: String) {
        binding.progressDownload.visibility = View.VISIBLE
        tsdListener?.remove()

        val ref = db.collection("tsd_bookings").document(tsdId)
        tsdListener = ref.addSnapshotListener { snap, err ->
            binding.progressDownload.visibility = View.GONE
            if (err != null) {
                Toast.makeText(requireContext(), "Error loading report: ${err.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            if (snap == null || !snap.exists()) {
                Toast.makeText(requireContext(), "Report not found", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            val m = snap.data ?: emptyMap<String, Any>()

            // bookingId
            val bookingId = (m["bookingId"] as? String) ?: snap.id
            binding.txtBookingId.text = bookingId
            binding.txtReportRef.text = "Ref: $bookingId"

            // status (optional)
            binding.txtSmallStatus.text = (m["bookingStatus"] as? String) ?: (m["status"] as? String) ?: ""

            // facilityName -> company header (and providerName field)
            val facilityName = (m["facilityName"] as? String).orEmpty()
            binding.txtCompanyName.text = facilityName
            binding.txtProviderName.text = facilityName // show facility name in provider slot for TSD POV

            // contact number
            binding.txtProviderContact.text = (m["contactNumber"] as? String)
                ?: (m["providerContact"] as? String)
                        ?: ""

            // location -> company address & explicit txtLocation
            val location = (m["location"] as? String).orEmpty()
            binding.txtCompanyAddress.text = location
            binding.txtLocation.text = location

            // booking date (dateCreated canonical)
            val bookingTs = (m["dateCreated"] as? Timestamp)
                ?: (m["bookingDate"] as? Timestamp)
                ?: (m["dateBooked"] as? Timestamp)
            binding.txtBookingDate.text = bookingTs?.toDate()?.let {
                DateFormat.format("MMM dd, yyyy • hh:mm a", it).toString()
            } ?: ""

            // quantity
            binding.txtQuantity.text = when (val q = m["quantity"]) {
                is Number -> q.toString()
                is String -> q
                else -> ""
            }

            // payment: rate or totalPayment -> txtPayment
            val rate = (m["rate"] as? Number)?.toDouble()
            val total = (m["totalPayment"] as? Number)?.toDouble()
            binding.txtPayment.text = when {
                total != null -> "₱${total.toInt()}"
                rate != null -> "₱${rate.toInt()}"
                else -> "₱0"
            }

            // treatmentInfo -> txtTreatmentInfo
            binding.txtTreatmentInfo.text = (m["treatmentInfo"] as? String) ?: ""

            // Attachments: prefer collectionProof array then certificateUrl / previousRecordUrl
            val attachments = mutableListOf<String>()
            (m["collectionProof"] as? List<*>)?.mapNotNull { it as? String }?.let { attachments.addAll(it) }
            (m["certificateUrl"] as? String)?.let { attachments.add(it) }
            (m["previousRecordUrl"] as? String)?.let { attachments.add(it) }
            (m["attachments"] as? List<*>)?.mapNotNull { it as? String }?.let { attachments.addAll(it) }
            (m["fileUrl"] as? String)?.let { attachments.add(it) }
            (m["attachmentUrl"] as? String)?.let { attachments.add(it) }

            if (attachments.isEmpty()) attachments.add(DEV_FALLBACK)

            currentAttachmentUrl = attachments.firstOrNull()
            currentFileName = currentAttachmentUrl?.substringAfterLast("/")?.substringBefore("?") ?: "attachment"
            binding.txtFileName.text = currentFileName
            binding.txtFileSize.text = "" // optional

            // show thumbnail if image, otherwise PDF icon
            if (isImageUrl(currentAttachmentUrl)) {
                Glide.with(this).load(currentAttachmentUrl).into(binding.imgFileIcon)
            } else {
                binding.imgFileIcon.setImageResource(R.drawable.ic_pdf)
            }
        }
    }

    private fun isImageUrl(url: String?): Boolean {
        if (url == null) return false
        val u = url.lowercase()
        return u.endsWith(".jpg") || u.endsWith(".jpeg") || u.endsWith(".png") || u.contains("image")
    }

    private fun onDownloadClicked() {
        val url = currentAttachmentUrl
        if (url.isNullOrBlank()) {
            Toast.makeText(requireContext(), "No document attached", Toast.LENGTH_SHORT).show()
            return
        }

        // open external viewer first
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (_: Exception) {}

        // also enqueue download
        try {
            val dm = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val req = DownloadManager.Request(Uri.parse(url))
                .setTitle(currentFileName ?: "file")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, currentFileName)

            dm.enqueue(req)
            Toast.makeText(requireContext(), "Downloading…", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tsdListener?.remove()
        _binding = null
    }
}
