package com.ecocp.capstoneenvirotrack.view.emb.opms

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentPtoDetails2Binding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class PtoDetailsFragment : Fragment() {

    private var _binding: FragmentPtoDetails2Binding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var applicationId: String? = null
    private var selectedFileUri: Uri? = null

    companion object {
        private const val PICK_FILE_REQUEST = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPtoDetails2Binding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        applicationId = arguments?.getString("applicationId")

        if (applicationId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No Application ID provided", Toast.LENGTH_SHORT).show()
        } else {
            loadApplicationDetails()
        }

        binding.btnApprove.setOnClickListener { updateStatus("Approved") }
        binding.btnReject.setOnClickListener { updateStatus("Rejected") }

        // ✅ Upload Certificate button logic
        binding.btnUploadCertificate.setOnClickListener {
            openFilePicker()
        }

        return binding.root
    }

    // ------------------------------------------------------------
    // LOAD DETAILS FROM FIRESTORE
    // ------------------------------------------------------------
    private fun loadApplicationDetails() {
        val id = applicationId ?: return

        db.collection("opms_pto_applications").document(id)
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (doc.exists()) {
                    Log.d("PTO_DETAILS", "✅ Document data: ${doc.data}")

                    val amount = doc.getDouble("amount") ?: 0.0
                    val currency = doc.getString("currency") ?: "PHP"
                    val paymentMethod = doc.getString("paymentMethod") ?: "-"
                    val paymentStatus = doc.getString("paymentStatus") ?: "Pending"
                    val dateFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
                    val paymentTs = doc.getTimestamp("paymentTimestamp")?.toDate()
                    val submittedTs = doc.getTimestamp("submittedTimestamp")?.toDate()
                        ?: doc.getTimestamp("timestamp")?.toDate()

                    val status = doc.getString("status") ?: "Pending"
                    val feedback = doc.getString("feedback") ?: ""

                    binding.apply {
                        // 🔹 Basic Info
                        txtOwnerName.text = doc.getString("ownerName") ?: "-"
                        txtEstablishmentName.text = doc.getString("establishmentName") ?: "-"
                        txtNatureOfBusiness.text = doc.getString("natureOfBusiness") ?: "-"
                        txtTin.text = doc.getString("tin") ?: "-"
                        txtOwnershipType.text = doc.getString("ownershipType") ?: "-"
                        txtMailingAddress.text = doc.getString("mailingAddress") ?: "-"
                        txtPlantAddress.text = doc.getString("plantAddress") ?: "-"
                        txtPcoName.text = doc.getString("pcoName") ?: "-"
                        txtPcoAccreditation.text = doc.getString("pcoAccreditation") ?: "-"
                        txtOperatingHours.text = doc.getString("operatingHours") ?: "-"
                        txtTotalEmployees.text = doc.getString("totalEmployees") ?: "-"
                        txtLandArea.text = doc.getString("landArea") ?: "-"
                        txtEquipmentName.text = doc.getString("equipmentName") ?: "-"
                        txtFuelType.text = doc.getString("fuelType") ?: "-"
                        txtEmissionsSummary.text = doc.getString("emissionsSummary") ?: "-"

                        // 🔹 Payment Display
                        txtAmount.text = "₱%.2f %s".format(amount, currency)
                        txtPaymentMethod.text = "Method: $paymentMethod"
                        txtPaymentStatus.text = "Status: $paymentStatus"
                        txtPaymentTimestamp.text = "Paid on: ${paymentTs?.let { dateFormat.format(it) } ?: "Not paid"}"
                        txtSubmittedTimestamp.text = "Submitted on: ${submittedTs?.let { dateFormat.format(it) } ?: "Not submitted"}"

                        // 🔹 Uploaded Files
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

                        txtStatus.text = "Status: $status"

                        // ✅ Show upload button only if Pending
                        btnUploadCertificate.visibility = if (status.equals("Pending", ignoreCase = true)) View.VISIBLE else View.GONE

                        // ✅ Feedback visibility
                        when (status.lowercase(Locale.getDefault())) {
                            "approved", "rejected" -> {
                                btnApprove.visibility = View.GONE
                                btnReject.visibility = View.GONE
                                inputFeedback.visibility = View.VISIBLE
                                inputFeedback.setText(feedback.ifBlank { "No feedback provided." })
                                inputFeedback.isEnabled = false
                                inputFeedback.setTextColor(resources.getColor(android.R.color.darker_gray))
                            }
                            else -> {
                                btnApprove.visibility = View.VISIBLE
                                btnReject.visibility = View.VISIBLE
                                inputFeedback.visibility = View.VISIBLE
                                inputFeedback.isEnabled = true
                                inputFeedback.setText(feedback)
                            }
                        }
                    }

                } else {
                    binding.txtOwnerName.text = "No details found for this PTO."
                }
            }
            .addOnFailureListener { e ->
                if (isAdded && _binding != null)
                    binding.txtOwnerName.text = "Error loading PTO details: ${e.message}"
                Log.e("PTO_DETAILS", "❌ Failed to load document", e)
            }
    }

    // ------------------------------------------------------------
    // FILE LINK DISPLAY
    // ------------------------------------------------------------
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

    // ------------------------------------------------------------
    // FILE PICKER
    // ------------------------------------------------------------
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        startActivityForResult(Intent.createChooser(intent, "Select Certificate PDF"), PICK_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedFileUri = data?.data
            selectedFileUri?.let { uri ->
                // Show selected file name
                val fileName = getFileNameFromUri(uri)
                    ?: uri.lastPathSegment?.substringAfterLast('/') ?: "selected_file.pdf"
                binding.tvSelectedFile.text = "Selected: $fileName"
                // Upload to Firebase
                uploadCertificateToFirebase(uri)
            }
        }
    }

    // Upload certificate and save certificateUrl to Firestore under this PTO doc
    private fun uploadCertificateToFirebase(fileUri: Uri) {
        val id = applicationId ?: return
        // Save under certificates/pto/{applicationId}_{timestamp}.pdf
        val storageFileName = "pto_${id}_${System.currentTimeMillis()}.pdf"
        val storageRef = storage.reference.child("certificates/pto/$storageFileName")

        storageRef.putFile(fileUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    // Save URL to Firestore
                    db.collection("opms_pto_applications").document(id)
                        .update("certificateUrl", downloadUrl)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Certificate uploaded successfully!", Toast.LENGTH_SHORT).show()
                            // Optional: hide button or update UI
                            binding.btnUploadCertificate.visibility = View.GONE
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to save certificate URL: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to get download URL: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Helper function to get file name from Uri (optional)
    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex("_display_name")
                if (index != -1) name = cursor.getString(index)
            }
        }
        return name
    }


    // ------------------------------------------------------------
    // APPROVE / REJECT STATUS UPDATE
    // ------------------------------------------------------------
    private fun updateStatus(status: String) {
        val id = applicationId ?: return
        val feedback = binding.inputFeedback.text.toString().trim()
        val embUid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔹 Prepare base data to update
        val updateData = mutableMapOf<String, Any>(
            "status" to status,
            "feedback" to feedback,
            "reviewedTimestamp" to Timestamp.now()
        )

        // 🔹 If approved, set issueDate and expiryDate (+5 years)
        if (status.equals("Approved", ignoreCase = true)) {
            val issueDate = Date()
            val calendar = Calendar.getInstance().apply {
                time = issueDate
                add(Calendar.YEAR, 5) // Add 5 years for expiry
            }
            val expiryDate = calendar.time

            updateData["issueDate"] = Timestamp(issueDate)
            updateData["expiryDate"] = Timestamp(expiryDate)
        }

        db.collection("opms_pto_applications").document(id)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Application $status successfully!", Toast.LENGTH_SHORT).show()

                if (isAdded) {
                    val navController = requireActivity().findNavController(R.id.embopms_nav_host_fragment)
                    navController.popBackStack(R.id.opmsEmbDashboardFragment, false)
                }

                // ✅ Notifications after update
                db.collection("opms_pto_applications").document(id).get()
                    .addOnSuccessListener { doc ->
                        val pcoUid = doc.getString("uid") ?: return@addOnSuccessListener
                        val companyName = doc.getString("establishmentName") ?: "Unknown Establishment"
                        val isApproved = status.equals("Approved", ignoreCase = true)

                        val notificationForPCO = hashMapOf(
                            "receiverId" to pcoUid,
                            "receiverType" to "pco",
                            "senderId" to embUid,
                            "title" to if (isApproved) "PTO Application Approved" else "PTO Application Rejected",
                            "message" to if (isApproved)
                                "Your Permit to Operate application has been approved. Valid for 5 years."
                            else
                                "Your Permit to Operate application has been rejected. Please review the feedback.",
                            "timestamp" to Timestamp.now(),
                            "isRead" to false,
                            "applicationId" to id
                        )

                        val notificationForEMB = hashMapOf(
                            "receiverId" to embUid,
                            "receiverType" to "emb",
                            "senderId" to embUid,
                            "title" to "PTO Application ${status.uppercase()}",
                            "message" to "You have $status a PTO application for $companyName.",
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
                Log.e("PTO_REVIEW", "❌ Failed to update PTO status", e)
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
