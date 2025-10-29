package com.ecocp.capstoneenvirotrack.view.emb.pcoacc

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.databinding.FragmentPcoReviewDetailsBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class PcoEmbReviewDetailsFragment : Fragment() {

    private var _binding: FragmentPcoReviewDetailsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var applicationId: String? = null
    private var uploadedCertificateUrl: String? = null
    private val PICK_FILE_REQUEST = 2001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPcoReviewDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applicationId = arguments?.getString("applicationId")
        if (applicationId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Application ID missing", Toast.LENGTH_SHORT).show()
            return
        }

        loadPcoDetails(applicationId!!)

        // ------------------------------------------------------------
        // FILE PICKER
        // ------------------------------------------------------------
        binding.btnUploadCertificate.setOnClickListener { openFilePicker() }

        // ------------------------------------------------------------
        // APPROVE / REJECT
        // ------------------------------------------------------------
        binding.btnApprove.setOnClickListener {
            if (uploadedCertificateUrl.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please upload the accreditation certificate first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            updateStatus("Approved", uploadedCertificateUrl!!)
        }

        binding.btnReject.setOnClickListener { updateStatus("Rejected", null) }
    }

    private fun loadPcoDetails(id: String) {
        db.collection("accreditations").document(id)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(requireContext(), "Application not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                binding.txtFullName.text = doc.getString("fullName") ?: "-"
                binding.txtAccreditationNumber.text = doc.getString("accreditationNumber") ?: "-"
                binding.txtCompanyAffiliation.text = doc.getString("companyAffiliation") ?: "-"
                binding.txtEducationalBackground.text = doc.getString("educationalBackground") ?: "-"
                binding.txtPositionDesignation.text = doc.getString("positionDesignation") ?: "-"
                binding.txtExperienceInEnvManagement.text = doc.getString("experienceInEnvManagement") ?: "-"

                val submittedTimestampField = doc.get("submittedTimestamp")
                val formattedSubmittedDate = when (submittedTimestampField) {
                    is Timestamp -> android.text.format.DateFormat.format("MMMM d, yyyy", submittedTimestampField.toDate())
                    is String -> submittedTimestampField
                    else -> "-"
                }
                binding.txtSubmittedTimestamp.text = formattedSubmittedDate.toString()
                val status = doc.getString("status") ?: "Pending"
                binding.txtStatus.text = status

                setupClickableLink(binding.linkCertificate, doc.getString("certificateUrl"))
                setupClickableLink(binding.linkGovernmentId, doc.getString("governmentIdUrl"))
                setupClickableLink(binding.linkTrainingCertificate, doc.getString("trainingCertificateUrl"))

                // ✅ Hide feedback if EMB didn't put any
                val feedback = doc.getString("feedback")
                if (feedback.isNullOrBlank()) {
                    binding.inputFeedback.visibility = View.GONE
                } else {
                    binding.inputFeedback.visibility = View.VISIBLE
                    binding.inputFeedback.setText(feedback)
                }

                if (status.equals("approved", true) || status.equals("rejected", true)) {
                    binding.btnApprove.visibility = View.GONE
                    binding.btnReject.visibility = View.GONE
                    binding.btnUploadCertificate.visibility = View.GONE
                    binding.tvSelectedFile.visibility = View.GONE
                } else {
                    binding.btnApprove.visibility = View.VISIBLE
                    binding.btnReject.visibility = View.VISIBLE
                    binding.btnUploadCertificate.visibility = View.VISIBLE
                    binding.tvSelectedFile.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                Log.e("PCO EMB", "Error loading details: ${e.message}")
                Toast.makeText(requireContext(), "Error loading PCO details.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun setupClickableLink(textView: android.widget.TextView, url: String?) {
        if (!url.isNullOrEmpty()) {
            textView.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Cannot open link", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            textView.text = "${textView.text} (Not uploaded)"
            textView.setTextColor(resources.getColor(android.R.color.darker_gray))
            textView.isClickable = false
        }
    }

    // ------------------------------------------------------------
    // FILE PICKER & UPLOAD
    // ------------------------------------------------------------
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        startActivityForResult(Intent.createChooser(intent, "Select Accreditation Certificate PDF"), PICK_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val fileUri = data.data ?: return
            val fileName = getFileNameFromUri(fileUri)
            binding.tvSelectedFile.text = fileName ?: "Unknown file"

            val pcoUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val storageFileName = "EMB_Accreditation_${System.currentTimeMillis()}.pdf"
            val storageRef = storage.reference.child("certificates/$pcoUid/$storageFileName")

            storageRef.putFile(fileUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        uploadedCertificateUrl = uri.toString()
                        db.collection("accreditations").document(applicationId!!)
                            .update("embCertificateUrl", uploadedCertificateUrl) // 🔹 Save under embCertificateUrl
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
            result = uri.path?.substringAfterLast('/')
        }
        return result
    }

    // ------------------------------------------------------------
    // APPROVE / REJECT + NOTIFICATIONS
    // ------------------------------------------------------------
    private fun updateStatus(status: String, certificateUrl: String?) {
        val id = applicationId ?: return
        val feedback = binding.inputFeedback.text.toString().trim()
        val embUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val updateData = mutableMapOf<String, Any>(
            "status" to status,
            "feedback" to feedback,
            "reviewedBy" to embUid,
            "reviewedTimestamp" to Timestamp.now()
        )

        if (status.equals("approved", true)) {
            val issueDate = Calendar.getInstance()
            val expiryDate = Calendar.getInstance().apply { add(Calendar.YEAR, 3) }
            updateData["issueDate"] = Timestamp(issueDate.time)
            updateData["expiryDate"] = Timestamp(expiryDate.time)
            certificateUrl?.let { updateData["certificateUrl"] = it }
        }

        db.collection("accreditations").document(id)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Application $status successfully!", Toast.LENGTH_SHORT).show()
                sendNotifications(id, status, embUid)
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("PCO_REVIEW", "Error updating status", e)
            }
    }

    private fun sendNotifications(id: String, status: String, embUid: String) {
        db.collection("accreditations").document(id).get()
            .addOnSuccessListener { doc ->
                val pcoUid = doc.getString("uid") ?: return@addOnSuccessListener
                val pcoName = doc.getString("fullName") ?: "Unknown PCO"
                val isApproved = status.equals("Approved", true)

                val notifForPCO = hashMapOf(
                    "receiverId" to pcoUid,
                    "receiverType" to "pco",
                    "senderId" to embUid,
                    "title" to if (isApproved) "Accreditation Approved" else "Accreditation Rejected",
                    "message" to if (isApproved)
                        "Your PCO Accreditation has been approved and will expire in 3 years."
                    else
                        "Your PCO Accreditation has been rejected. Please review the feedback.",
                    "timestamp" to Timestamp.now(),
                    "isRead" to false,
                    "applicationId" to id
                )

                val notifForEMB = hashMapOf(
                    "receiverId" to embUid,
                    "receiverType" to "emb",
                    "senderId" to embUid,
                    "title" to "Accreditation ${status.uppercase()}",
                    "message" to "You have $status a PCO accreditation application for $pcoName.",
                    "timestamp" to Timestamp.now(),
                    "isRead" to false,
                    "applicationId" to id
                )

                db.collection("notifications").add(notifForPCO)
                db.collection("notifications").add(notifForEMB)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
