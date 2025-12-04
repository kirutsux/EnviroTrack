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
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentDpDetailsBinding
import com.ecocp.capstoneenvirotrack.utils.NotificationManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

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
                    txtStatus.text = doc.getString("status") ?: "-"

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

                    // populate local var if certificate was already uploaded earlier
                    uploadedCertificateUrl = certificateUrl

                    when (status) {
                        "approved" -> {
                            btnApprove.visibility = View.GONE
                            btnReject.visibility = View.GONE
                            // EMB can still view upload button only if no certificate exists (but as per your request upload visible while pending,
                            // so for approved we hide upload if cert exists, otherwise leave it visible)
                            inputFeedback.visibility = View.VISIBLE
                            inputFeedback.setText(feedback.ifBlank { "No feedback provided." })
                            inputFeedback.isEnabled = false
                            inputFeedback.setTextColor(resources.getColor(android.R.color.darker_gray))

                            if (certificateUrl.isNullOrBlank()) {
                                // in rare case approved but no certificate yet -> allow upload
                                btnUploadCertificate.visibility = View.VISIBLE
                                tvSelectedFile.text = "No file selected"
                            } else {
                                btnUploadCertificate.visibility = View.GONE
                                tvSelectedFile.text = "Uploaded: ${certificateUrl.substringAfterLast("/").substringBefore("?")}"
                            }
                        }
                        "rejected" -> {
                            btnApprove.visibility = View.GONE
                            btnReject.visibility = View.GONE
                            btnUploadCertificate.visibility = View.GONE
                            inputFeedback.visibility = View.VISIBLE
                            inputFeedback.setText(feedback.ifBlank { "No feedback provided." })
                            inputFeedback.isEnabled = false
                            inputFeedback.setTextColor(resources.getColor(android.R.color.darker_gray))
                            tvSelectedFile.text = "No file selected"
                        }
                        else -> { // pending -> show upload button (EMB can upload while pending)
                            btnApprove.visibility = View.VISIBLE
                            btnReject.visibility = View.VISIBLE
                            btnUploadCertificate.visibility = View.VISIBLE
                            inputFeedback.visibility = View.VISIBLE
                            inputFeedback.isEnabled = true
                            inputFeedback.setText(feedback)
                            // show filename if already uploaded, otherwise default text
                            if (!certificateUrl.isNullOrBlank()) {
                                tvSelectedFile.text = "Uploaded: ${certificateUrl.substringAfterLast("/").substringBefore("?")}"
                            } else {
                                tvSelectedFile.text = "No file selected"
                            }
                        }
                    }

                    // Display uploaded application files (fileLinks) if any
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
    private fun updateStatus(status: String) {
        val id = applicationId ?: return
        val embUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val feedback = binding.inputFeedback.text.toString().trim()

        // Always check fragment state
        if (!isAdded || context == null) return
        val safeContext = requireContext()

        val updateData = mutableMapOf<String, Any>(
            "status" to status,
            "feedback" to feedback,
            "reviewedTimestamp" to Timestamp.now()
        )

        // Discharge Permit applications only
        val collectionName = "opms_discharge_permits"

        // Update Firestore first
        db.collection(collectionName).document(id)
            .update(updateData)
            .addOnSuccessListener {

                if (isAdded) {
                    Toast.makeText(safeContext, "Application $status successfully!", Toast.LENGTH_SHORT).show()
                }

                // Reload details for immediate UI update
                loadDischargePermitDetails()

                // Fetch PCO uid from document
                db.collection(collectionName).document(id).get()
                    .addOnSuccessListener { doc ->
                        val pcoId = doc.getString("uid") ?: return@addOnSuccessListener

                        // ⚠ Call backend endpoint for status update (not send-notification)
                        val url = "http://10.0.2.2:5000/update-status"
                        val json = JSONObject().apply {
                            put("applicationId", id)
                            put("newStatus", status)
                            put("pcoId", pcoId)
                            put("embId", embUid)
                            put("module", "DISCHARGE")
                            put("feedback", feedback)
                        }

                        Volley.newRequestQueue(safeContext).add(
                            JsonObjectRequest(Request.Method.POST, url, json,
                                { /* success */ },
                                { error ->
                                    Toast.makeText(safeContext, "Failed to notify: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        )

                        // Navigate back safely
                        if (isAdded) {
                            val navController = requireActivity().findNavController(R.id.embopms_nav_host_fragment)
                            navController.popBackStack(R.id.opmsEmbDashboardFragment, false)
                        }
                    }
            }
            .addOnFailureListener {
                if (isAdded) {
                    Toast.makeText(safeContext, "Failed to update status: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
