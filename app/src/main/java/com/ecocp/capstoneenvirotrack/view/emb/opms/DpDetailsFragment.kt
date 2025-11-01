package com.ecocp.capstoneenvirotrack.view.emb.opms

import android.app.Activity
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
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentDpDetailsBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.*

class DpDetailsFragment : Fragment() {

    private var _binding: FragmentDpDetailsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var applicationId: String? = null
    private var selectedFileUri: Uri? = null
    private var uploadedCertificateUrl: String? = null

    companion object {
        private const val PICK_FILE_REQUEST = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDpDetailsBinding.inflate(inflater, container, false)
        applicationId = arguments?.getString("applicationId")

        if (applicationId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No Application ID provided", Toast.LENGTH_SHORT).show()
            Log.e("DP_DETAILS", "❌ applicationId is null or empty")
        } else {
            Log.d("DP_DETAILS", "🔍 Loading details for ID: $applicationId")
            loadDischargePermitDetails()
        }

        // Approve / Reject buttons
        binding.btnApprove.setOnClickListener {
            // require certificate before approving
            if (uploadedCertificateUrl.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Please upload a certificate before approving.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            updateStatus("Approved")
        }
        binding.btnReject.setOnClickListener { updateStatus("Rejected") }

        // Upload certificate button click
        binding.btnUploadCertificate.setOnClickListener {
            openFileChooser()
        }

        return binding.root
    }

    // Open system file chooser (PDF)
    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, "Select Certificate"), PICK_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedFileUri = data?.data
            selectedFileUri?.let { uri ->
                // show selected file name
                val fileName = getFileNameFromUri(uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "selected_file.pdf"
                binding.tvSelectedFile.text = "Selected: $fileName"
                // upload
                uploadCertificateToFirebase(uri)
            }
        }
    }

    // Upload certificate and save certificateUrl to Firestore under this DP doc
    private fun uploadCertificateToFirebase(fileUri: Uri) {
        val id = applicationId ?: return
        // Save under certificates/discharge_permits/{applicationId}_{timestamp}.pdf
        val storageFileName = "discharge_permit_${id}_${System.currentTimeMillis()}.pdf"
        val storageRef = storage.reference.child("certificates/discharge_permits/$storageFileName")

        val uploadTask = storageRef.putFile(fileUri)
        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val downloadUrl = uri.toString()
                // Save URL to document and set local variable
                db.collection("opms_discharge_permits").document(id)
                    .update("certificateUrl", downloadUrl)
                    .addOnSuccessListener {
                        uploadedCertificateUrl = downloadUrl
                        Toast.makeText(requireContext(), "Certificate uploaded successfully!", Toast.LENGTH_SHORT).show()
                        // reflect change in UI (filename already shown)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to save certificate URL: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to get download URL: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
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

    // Load DP details and set visibility of upload button (upload should be visible while pending)
    private fun loadDischargePermitDetails() {
        val id = applicationId ?: return

        db.collection("opms_discharge_permits").document(id)
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                if (!doc.exists()) {
                    binding.txtCompanyName.text = "No details found."
                    return@addOnSuccessListener
                }

                binding.apply {
                    txtCompanyName.text = doc.getString("companyName") ?: "-"
                    txtCompanyAddress.text = doc.getString("companyAddress") ?: "-"
                    txtPcoName.text = doc.getString("pcoName") ?: "-"
                    txtAccreditation.text = doc.getString("pcoAccreditation") ?: "-"
                    txtContactNumber.text = doc.getString("contactNumber") ?: "-"
                    txtEmail.text = doc.getString("email") ?: "-"
                    txtBodyOfWater.text = doc.getString("bodyOfWater") ?: "-"
                    txtSourceWastewater.text = doc.getString("sourceWastewater") ?: "-"
                    txtVolume.text = doc.getString("volume") ?: "-"
                    txtTreatment.text = doc.getString("treatmentMethod") ?: "-"
                    txtOperationDate.text = doc.getString("operationStartDate") ?: "-"

                    // Payment info
                    val amount = doc.getDouble("amount") ?: 0.0
                    val currency = doc.getString("currency") ?: "PHP"
                    val paymentMethod = doc.getString("paymentMethod") ?: "-"
                    val paymentTimestamp = doc.getTimestamp("paymentTimestamp")
                    val submittedTimestamp = doc.getTimestamp("submittedTimestamp")

                    val paidOn = paymentTimestamp?.toDate()?.toString() ?: "-"
                    val submittedOn = submittedTimestamp?.toDate()?.toString() ?: "-"

                    txtAmount.text = "₱%.2f %s".format(amount, currency)
                    txtPaymentMethod.text = "Method: $paymentMethod"
                    txtPaymentStatus.text = "Status: Paid"
                    txtPaymentTimestamp.text = "Paid on: $paidOn"
                    txtSubmittedTimestamp.text = "Submitted on: $submittedOn"

                    val status = doc.getString("status")?.lowercase(Locale.getDefault()) ?: "pending"
                    val feedback = doc.getString("feedback") ?: ""
                    val certificateUrl = doc.getString("certificateUrl")

                    // store uploaded certificate URL (if any)
                    uploadedCertificateUrl = certificateUrl

                    txtStatus.text = "Status: ${status.replaceFirstChar { it.uppercase() }}"

                    when (status) {
                        "approved" -> {
                            // Hide upload section
                            btnUploadCertificate.visibility = View.GONE
                            tvSelectedFile.visibility = View.GONE

                            // Hide review buttons
                            btnApprove.visibility = View.GONE
                            btnReject.visibility = View.GONE

                            // Feedback readonly
                            inputFeedback.visibility = View.VISIBLE
                            inputFeedback.setText(feedback.ifBlank { "No feedback provided." })
                            inputFeedback.isEnabled = false
                            inputFeedback.setTextColor(resources.getColor(android.R.color.darker_gray))
                        }

                        "rejected" -> {
                            // Hide upload section
                            btnUploadCertificate.visibility = View.GONE
                            tvSelectedFile.visibility = View.GONE

                            // Hide review buttons
                            btnApprove.visibility = View.GONE
                            btnReject.visibility = View.GONE

                            // Feedback readonly
                            inputFeedback.visibility = View.VISIBLE
                            inputFeedback.setText(feedback.ifBlank { "No feedback provided." })
                            inputFeedback.isEnabled = false
                            inputFeedback.setTextColor(resources.getColor(android.R.color.darker_gray))
                        }

                        else -> { // pending
                            // Show upload section only for pending
                            btnUploadCertificate.visibility = View.VISIBLE
                            tvSelectedFile.visibility = View.VISIBLE

                            // Show review controls
                            btnApprove.visibility = View.VISIBLE
                            btnReject.visibility = View.VISIBLE
                            inputFeedback.visibility = View.VISIBLE
                            inputFeedback.isEnabled = true
                            inputFeedback.setText(feedback)

                            // show filename if already uploaded
                            if (!certificateUrl.isNullOrBlank()) {
                                tvSelectedFile.text = "Uploaded: ${certificateUrl.substringAfterLast("/").substringBefore("?")}"
                            } else {
                                tvSelectedFile.text = "No file selected"
                            }
                        }
                    }

                    // Display uploaded application files (fileLinks)
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
                }
            }
            .addOnFailureListener { e ->
                if (isAdded && _binding != null)
                    binding.txtCompanyName.text = "Error loading details: ${e.message}"
            }
    }


    // Show clickable file links
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
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to open file", Toast.LENGTH_SHORT).show()
        }
    }

    // Approve / Reject + Notifications
    // Approve / Reject + Notifications
    private fun updateStatus(status: String) {
        val id = applicationId ?: return
        val feedback = binding.inputFeedback.text.toString().trim()
        val embUid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Common fields for update
        val updateData = mutableMapOf<String, Any>(
            "status" to status,
            "feedback" to feedback,
            "reviewedTimestamp" to Timestamp.now()
        )

        // ✅ If approved → add issueDate and expiryDate
        if (status.equals("Approved", ignoreCase = true)) {
            val issueDate = Timestamp.now()
            val calendar = Calendar.getInstance().apply { time = issueDate.toDate() }
            calendar.add(Calendar.YEAR, 5)
            val expiryDate = Timestamp(calendar.time)
            updateData["issueDate"] = issueDate
            updateData["expiryDate"] = expiryDate
        }

        db.collection("opms_discharge_permits").document(id)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Application $status successfully!", Toast.LENGTH_SHORT).show()

                // Reload details to refresh UI
                loadDischargePermitDetails()

                // Navigate back to dashboard
                if (isAdded) {
                    val navController = requireActivity().findNavController(R.id.embopms_nav_host_fragment)
                    navController.popBackStack(R.id.opmsEmbDashboardFragment, false)
                }

                // ✅ Notifications
                db.collection("opms_discharge_permits").document(id).get()
                    .addOnSuccessListener { doc ->
                        val pcoUid = doc.getString("uid") ?: return@addOnSuccessListener
                        val companyName = doc.getString("companyName") ?: "Unknown Establishment"
                        val isApproved = status.equals("Approved", ignoreCase = true)

                        val notificationForPCO = hashMapOf(
                            "receiverId" to pcoUid,
                            "receiverType" to "pco",
                            "senderId" to embUid,
                            "title" to if (isApproved) "Discharge Permit Approved" else "Discharge Permit Rejected",
                            "message" to if (isApproved)
                                "Your Discharge Permit application has been approved."
                            else
                                "Your Discharge Permit application has been rejected. Please review the feedback.",
                            "timestamp" to Timestamp.now(),
                            "isRead" to false,
                            "applicationId" to id
                        )

                        val notificationForEMB = hashMapOf(
                            "receiverId" to embUid,
                            "receiverType" to "emb",
                            "senderId" to embUid,
                            "title" to "Discharge Permit ${status.uppercase()}",
                            "message" to "You have $status a Discharge Permit for $companyName.",
                            "timestamp" to Timestamp.now(),
                            "isRead" to false,
                            "applicationId" to id
                        )

                        db.collection("notifications").add(notificationForPCO)
                        db.collection("notifications").add(notificationForEMB)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update status: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("DP_REVIEW", "❌ Failed to update DP status", e)
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
