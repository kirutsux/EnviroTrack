package com.ecocp.capstoneenvirotrack.view.emb.cnc

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.ecocp.capstoneenvirotrack.databinding.FragmentCncReviewDetailsBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.storage.FirebaseStorage

class CncReviewDetailsFragment : Fragment() {

    private var _binding: FragmentCncReviewDetailsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var applicationId: String? = null
    private var uploadedCertificateUrl: String? = null  // 🟩 store the uploaded certificate link

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCncReviewDetailsBinding.inflate(inflater, container, false)

        applicationId = arguments?.getString("applicationId")

        if (applicationId != null) {
            fetchCncDetails(applicationId!!)
            setupButtons()
        } else {
            Toast.makeText(requireContext(), "No application selected", Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }

    private fun fetchCncDetails(applicationId: String) {
        db.collection("cnc_applications").document(applicationId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(requireContext(), "Application not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // 🔹 Section A — Basic Information
                binding.txtCompanyName.text = doc.getString("companyName") ?: "-"
                binding.txtBusinessName.text = doc.getString("businessName") ?: "-"
                binding.txtProjectTitle.text = doc.getString("projectTitle") ?: "-"
                binding.txtNatureOfBusiness.text = doc.getString("natureOfBusiness") ?: "-"
                binding.txtProjectLocation.text = doc.getString("projectLocation") ?: "-"
                binding.txtEmail.text = doc.getString("email") ?: "-"
                binding.txtManagingHead.text = doc.getString("managingHead") ?: "-"
                binding.txtPcoName.text = doc.getString("pcoName") ?: "-"
                binding.txtPcoAccreditation.text = doc.getString("pcoAccreditation") ?: "-"
                binding.txtDateEstablished.text = doc.getString("dateEstablished") ?: "-"
                binding.txtNumEmployees.text = doc.getString("numEmployees") ?: "-"
                binding.txtPsicCode.text = doc.getString("psicCode") ?: "-"

                // 🔹 Section B — Project Description
                binding.txtProjectType.text = doc.getString("projectType") ?: "-"
                binding.txtProjectScale.text = doc.getString("projectScale") ?: "-"
                binding.txtProjectCost.text = doc.getString("projectCost") ?: "-"
                binding.txtLandArea.text = doc.getString("landArea") ?: "-"
                binding.txtRawMaterials.text = doc.getString("rawMaterials") ?: "-"
                binding.txtProductionCapacity.text = doc.getString("productionCapacity") ?: "-"
                binding.txtUtilitiesUsed.text = doc.getString("utilitiesUsed") ?: "-"
                binding.txtWasteGenerated.text = doc.getString("wasteGenerated") ?: "-"

                // 🔹 Section C — Location and Environmental Setting
                binding.txtCoordinates.text = doc.getString("coordinates") ?: "-"
                binding.txtNearbyWaters.text = doc.getString("nearbyWaters") ?: "-"
                binding.txtResidentialProximity.text = doc.getString("residentialProximity") ?: "-"
                binding.txtEnvFeatures.text = doc.getString("envFeatures") ?: "-"
                binding.txtZoning.text = doc.getString("zoning") ?: "-"

                // 🔹 Section D — Uploaded Files
                val fileLinksField = doc.get("fileLinks")
                when (fileLinksField) {
                    is List<*> -> {
                        val fileLinks = fileLinksField.filterIsInstance<String>()
                        if (fileLinks.isNotEmpty()) displayFileLinks(fileLinks) else addEmptyFileNotice()
                    }
                    is String -> {
                        val fileLinks = fileLinksField.split(",").map { it.trim() }
                        displayFileLinks(fileLinks)
                    }
                    else -> addEmptyFileNotice()
                }

                // 🔹 Payment Information
                val amount = doc.getDouble("amount") ?: 0.0
                val currency = doc.getString("currency") ?: "PHP"
                val paymentMethod = doc.getString("paymentMethod") ?: "-"
                val paymentStatus = doc.getString("paymentStatus") ?: "Pending"

                val dateFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
                val paymentTs = doc.getTimestamp("paymentTimestamp")?.toDate()
                val submittedTs = doc.getTimestamp("submittedTimestamp")?.toDate()
                    ?: doc.getTimestamp("timestamp")?.toDate()

                binding.txtAmount.text = "₱%.2f %s".format(amount, currency)
                binding.txtPaymentMethod.text = "Method: $paymentMethod"
                binding.txtPaymentStatus.text = "Status: $paymentStatus"
                binding.txtPaymentTimestamp.text =
                    "Paid on: ${paymentTs?.let { dateFormat.format(it) } ?: "Not paid"}"
                binding.txtSubmittedTimestamp.text =
                    "Submitted on: ${submittedTs?.let { dateFormat.format(it) } ?: "Not submitted"}"

                // 🔹 Review Status Handling
                val status = doc.getString("status")?.lowercase(Locale.getDefault()) ?: "pending"
                val feedback = doc.getString("feedback") ?: ""

                when (status) {
                    "approved" -> {
                        // Hide upload certificate section
                        binding.btnUploadCertificate.visibility = View.GONE
                        binding.tvSelectedFile.visibility = View.GONE
                        binding.btnApprove.visibility = View.GONE
                        binding.btnReject.visibility = View.GONE
                        binding.inputFeedback.visibility = View.GONE
                    }
                    "rejected" -> {
                        binding.btnApprove.visibility = View.GONE
                        binding.btnReject.visibility = View.GONE
                        binding.btnUploadCertificate.visibility = View.GONE
                        binding.tvSelectedFile.visibility = View.GONE

                        // Show feedback as read-only for rejected applications
                        binding.inputFeedback.visibility = View.VISIBLE
                        binding.inputFeedback.setText(feedback.ifBlank { "No feedback provided." })
                        binding.inputFeedback.isEnabled = false
                        binding.inputFeedback.setTextColor(resources.getColor(android.R.color.darker_gray))
                    }
                    else -> {
                        // Show normal input and buttons for pending applications
                        binding.btnApprove.visibility = View.VISIBLE
                        binding.btnReject.visibility = View.VISIBLE
                        binding.btnUploadCertificate.visibility = View.VISIBLE
                        binding.tvSelectedFile.visibility = View.VISIBLE
                        binding.inputFeedback.visibility = View.VISIBLE
                        binding.inputFeedback.isEnabled = true
                        binding.inputFeedback.setText("")
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error loading CNC details.", Toast.LENGTH_SHORT).show()
            }
    }

    // 🔹 Show file links
    private fun displayFileLinks(fileLinks: List<String>) {
        binding.layoutFileLinks.removeAllViews()
        for ((index, link) in fileLinks.withIndex()) {
            val textView = TextView(requireContext()).apply {
                val fileName = link.substringAfterLast('/').substringBefore('?')
                text = if (fileName.isNotBlank()) fileName else "File ${index + 1}"
                setTextColor(resources.getColor(android.R.color.holo_blue_dark))
                textSize = 15f
                setPadding(8, 8, 8, 8)
                setOnClickListener { openFileLink(link) }
            }
            binding.layoutFileLinks.addView(textView)
        }
    }

    private fun addEmptyFileNotice() {
        val textView = TextView(requireContext()).apply {
            text = "No files uploaded."
            setTextColor(resources.getColor(android.R.color.darker_gray))
            textSize = 14f
        }
        binding.layoutFileLinks.addView(textView)
    }

    private fun openFileLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to open file", Toast.LENGTH_SHORT).show()
        }
    }

    // 🔹 BUTTONS
    private fun setupButtons() {
        // ✅ Upload certificate first
        binding.btnUploadCertificate.setOnClickListener {
            val filePicker = Intent(Intent.ACTION_GET_CONTENT).apply { type = "application/pdf" }
            startActivityForResult(Intent.createChooser(filePicker, "Select CNC Certificate PDF"), 1001)
        }

        // ✅ Approve after upload
        binding.btnApprove.setOnClickListener {
            if (uploadedCertificateUrl.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please upload a CNC certificate first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            updateStatus("Approved", "Application approved by EMB.", uploadedCertificateUrl!!)
        }

        // ✅ Reject
        binding.btnReject.setOnClickListener {
            val feedback = binding.inputFeedback.text.toString().trim()
            if (feedback.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter feedback before rejecting.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            updateStatus("Rejected", feedback, null)
        }
    }

    // 🔹 Handle certificate upload
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == android.app.Activity.RESULT_OK && data != null) {
            val fileUri = data.data ?: return
            val embUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

            val fileName = getFileNameFromUri(fileUri)
            binding.tvSelectedFile.text = fileName ?: "Unknown file"

            // Upload to Storage
            db.collection("cnc_applications").document(applicationId!!)
                .get()
                .addOnSuccessListener { doc ->
                    val pcoUid = doc.getString("uid") ?: return@addOnSuccessListener
                    val storageFileName = "CNC_Certificate_${System.currentTimeMillis()}.pdf"
                    val storageRef = FirebaseStorage.getInstance()
                        .reference.child("certificates/$pcoUid/$storageFileName")

                    storageRef.putFile(fileUri)
                        .addOnSuccessListener {
                            storageRef.downloadUrl.addOnSuccessListener { uri ->
                                uploadedCertificateUrl = uri.toString()

                                db.collection("cnc_applications").document(applicationId!!)
                                    .update("certificateUrl", uploadedCertificateUrl)
                                    .addOnSuccessListener {
                                        Toast.makeText(requireContext(), "Certificate uploaded successfully! You can now approve.", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) result = result?.substring(cut + 1)
        }
        return result
    }

    // 🔹 Update CNC status + notifications
    private fun updateStatus(status: String, feedback: String, certificateUrl: String?) {
        val id = applicationId ?: return
        val embUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val updateData = mutableMapOf<String, Any>(
            "status" to status,
            "feedback" to feedback,
            "reviewedTimestamp" to Timestamp.now()
        )
        certificateUrl?.let { updateData["certificateUrl"] = it }

        db.collection("cnc_applications").document(id)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Application $status successfully!", Toast.LENGTH_SHORT).show()

                // 🔔 Notifications
                db.collection("cnc_applications").document(id).get()
                    .addOnSuccessListener { doc ->
                        val pcoId = doc.getString("uid") ?: return@addOnSuccessListener
                        val companyName = doc.getString("companyName") ?: "Unknown Company"
                        val isApproved = status.equals("Approved", ignoreCase = true)

                        val notifPCO = hashMapOf(
                            "receiverId" to pcoId,
                            "receiverType" to "pco",
                            "senderId" to embUid,
                            "title" to if (isApproved) "Application Approved" else "Application Rejected",
                            "message" to if (isApproved)
                                "Your CNC application has been approved. Certificate is now available."
                            else
                                "Your CNC application has been rejected. Please review the feedback.",
                            "timestamp" to Timestamp.now(),
                            "isRead" to false,
                            "applicationId" to id
                        )

                        val notifEMB = hashMapOf(
                            "receiverId" to embUid,
                            "receiverType" to "emb",
                            "senderId" to embUid,
                            "title" to "CNC Application ${status.uppercase()}",
                            "message" to "You have $status a CNC application by $companyName.",
                            "timestamp" to Timestamp.now(),
                            "isRead" to false,
                            "applicationId" to id
                        )

                        db.collection("notifications").add(notifPCO)
                        db.collection("notifications").add(notifEMB)
                    }

                // ✅ Return to dashboard
                if (isAdded) {
                    val navController = requireActivity().findNavController(R.id.embcnc_nav_host_fragment)
                    navController.popBackStack(R.id.cncEmbDashboardFragment, false)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
