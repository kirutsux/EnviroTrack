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
import java.io.File

class SP_ServiceReport : Fragment() {

    private var _binding: FragmentSpServiceReportBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    private val DEV_FALLBACK = "/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png"

    private var currentAttachmentUrl: String? = null
    private var currentFileName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpServiceReportBinding.inflate(inflater, container, false)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnBackToList.setOnClickListener { findNavController().popBackStack() }

        binding.btnDownload.setOnClickListener { onDownloadClicked() }

        populateFromArgsBasic()
        fetchBookingAndPopulate()

        return binding.root
    }

    private fun populateFromArgsBasic() {
        val args = arguments
        args?.let {
            binding.txtCompanyName.text = it.getString("companyName", "")
            binding.txtRemarks.text = it.getString("status", "")
            val req = it.getString("requestId", "")
            if (req.isNotBlank()) binding.txtReportRef.text = "Ref: $req"
        }
    }

    private fun fetchBookingAndPopulate() {
        val args = arguments
        val id = args?.getString("requestId") ?: args?.getString("bookingId") ?: ""

        if (id.isBlank()) {
            Toast.makeText(requireContext(), "No booking ID provided", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressDownload.visibility = View.VISIBLE

        db.collection("transport_bookings").document(id).get()
            .addOnSuccessListener { doc ->
                binding.progressDownload.visibility = View.GONE
                val m = doc.data ?: return@addOnSuccessListener

                binding.txtBookingId.text = (m["bookingId"] as? String) ?: doc.id
                binding.txtReportRef.text = "Ref: ${(m["bookingId"] as? String) ?: doc.id}"

                val providerName = (m["serviceProviderCompany"] as? String)?.let { comp ->
                    val pname = (m["serviceProviderName"] as? String).orEmpty()
                    if (pname.isBlank()) comp else "$comp - $pname"
                } ?: (m["serviceProviderName"] as? String) ?: ""
                binding.txtProviderName.text = providerName
                binding.txtProviderContact.text = (m["providerContact"] as? String) ?: ""

                val bookingTs = (m["bookingDate"] as? Timestamp) ?: (m["dateBooked"] as? Timestamp)
                val completedTs =
                    (m["completedAt"] as? Timestamp)
                        ?: (m["assignedAt"] as? Timestamp)
                        ?: bookingTs

                binding.txtBookingDate.text = bookingTs?.toDate()?.let {
                    DateFormat.format("MMM dd, yyyy • hh:mm a", it)
                } ?: ""

                binding.txtCompletionDate.text = completedTs?.toDate()?.let {
                    DateFormat.format("MMM dd, yyyy • hh:mm a", it)
                } ?: ""




                binding.txtQuantity.text = (m["quantity"] as? String) ?: ""
                binding.txtPackaging.text = (m["packaging"] as? String) ?: ""
                binding.txtRemarks.text =
                    (m["specialInstructions"] as? String)
                        ?: (m["notes"] as? String)
                                ?: binding.txtRemarks.text

                val attachments = mutableListOf<String>()
                (m["collectionProof"] as? List<*>)?.mapNotNull { it as? String }?.let { attachments.addAll(it) }
                (m["finalReportUrl"] as? String)?.let { attachments.add(it) }
                (m["transportPlanUrl"] as? String)?.let { attachments.add(it) }
                (m["storagePermitUrl"] as? String)?.let { attachments.add(it) }
                (m["attachments"] as? List<*>)?.mapNotNull { it as? String }?.let { attachments.addAll(it) }

                if (attachments.isEmpty()) attachments.add(DEV_FALLBACK)

                currentAttachmentUrl = attachments.firstOrNull()
                currentFileName = extractFileNameFromUrl(currentAttachmentUrl ?: "")

                binding.txtFileName.text = currentFileName
                binding.txtFileSize.text = ""

                if (isImageUrl(currentAttachmentUrl)) {
                    Glide.with(this).load(currentAttachmentUrl).into(binding.imgFileIcon)
                } else {
                    binding.imgFileIcon.setImageResource(R.drawable.ic_pdf)
                }
            }
            .addOnFailureListener {
                binding.progressDownload.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed loading report", Toast.LENGTH_SHORT).show()
            }
    }

    private fun extractFileNameFromUrl(url: String): String {
        if (url.isBlank()) return "attachment"
        return url.substringAfterLast("/").substringBefore("?")
    }

    private fun isImageUrl(url: String?): Boolean {
        if (url == null) return false
        val u = url.lowercase()
        return u.endsWith(".jpg") || u.endsWith(".png") || u.contains("image")
    }

    private fun onDownloadClicked() {
        val url = currentAttachmentUrl ?: return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (_: Exception) {}

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
        _binding = null
    }
}
