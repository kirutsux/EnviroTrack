package com.ecocp.capstoneenvirotrack.view.emb.pcoacc

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.Timestamp
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.databinding.FragmentPcoReviewDetailsBinding
import com.ecocp.capstoneenvirotrack.utils.NotificationManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class PcoEmbReviewDetailsFragment : Fragment() {

    private var _binding: FragmentPcoReviewDetailsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private var applicationId: String? = null

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

        binding.btnApprove.setOnClickListener { updateStatus("Approved") }
        binding.btnReject.setOnClickListener { updateStatus("Rejected") }
    }

    private fun loadPcoDetails(id: String) {
        db.collection("accreditations").document(id)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(requireContext(), "Application not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // 🔹 Basic Information
                binding.txtFullName.text = doc.getString("fullName") ?: "-"
                binding.txtAccreditationNumber.text = doc.getString("accreditationNumber") ?: "-"
                binding.txtCompanyAffiliation.text = doc.getString("companyAffiliation") ?: "-"
                binding.txtEducationalBackground.text = doc.getString("educationalBackground") ?: "-"
                binding.txtPositionDesignation.text = doc.getString("positionDesignation") ?: "-"
                binding.txtExperienceInEnvManagement.text = doc.getString("experienceInEnvManagement") ?: "-"
                binding.txtSubmittedTimestamp.text = doc.getString("submittedTimestamp") ?: "-"
                binding.txtStatus.text = doc.getString("status") ?: "Pending"

                // 🔹 Set up document links
                setupClickableLink(binding.linkCertificate, doc.getString("certificateUrl"))
                setupClickableLink(binding.linkGovernmentId, doc.getString("governmentIdUrl"))
                setupClickableLink(binding.linkTrainingCertificate, doc.getString("trainingCertificateUrl"))

                // 🔹 Review Status & Feedback Handling
                val status = doc.getString("status")?.lowercase(Locale.getDefault()) ?: "pending"
                val feedback = doc.getString("feedback") ?: ""

                when (status) {
                    "approved" -> {
                        binding.btnApprove.visibility = View.GONE
                        binding.btnReject.visibility = View.GONE
                        binding.inputFeedback.visibility = View.VISIBLE
                        binding.inputFeedback.setText(
                            feedback.ifBlank { "No feedback provided." }
                        )
                        binding.inputFeedback.isEnabled = false
                        binding.inputFeedback.setTextColor(resources.getColor(android.R.color.darker_gray))
                    }
                    "rejected" -> {
                        binding.btnApprove.visibility = View.GONE
                        binding.btnReject.visibility = View.GONE
                        binding.inputFeedback.visibility = View.VISIBLE
                        binding.inputFeedback.setText(
                            feedback.ifBlank { "No feedback provided." }
                        )
                        binding.inputFeedback.isEnabled = false
                        binding.inputFeedback.setTextColor(resources.getColor(android.R.color.darker_gray))
                    }
                    else -> {
                        binding.btnApprove.visibility = View.VISIBLE
                        binding.btnReject.visibility = View.VISIBLE
                        binding.inputFeedback.visibility = View.VISIBLE
                        binding.inputFeedback.isEnabled = true
                        binding.inputFeedback.setText(feedback)
                    }
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
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
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

    private fun updateStatus(status: String) {
        val id = applicationId ?: return
        val feedback = binding.inputFeedback.text.toString().trim()
        val embUid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val updateData = mapOf(
            "status" to status,
            "feedback" to feedback,
            "reviewedBy" to embUid,
            "reviewedTimestamp" to Timestamp.now()
        )

        db.collection("accreditations").document(id)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Application $status successfully!", Toast.LENGTH_SHORT).show()

                if (isAdded) {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }

                // ✅ Send notifications using NotificationManager
                db.collection("accreditations").document(id).get()
                    .addOnSuccessListener { doc ->
                        val pcoUid = doc.getString("uid") ?: return@addOnSuccessListener
                        val pcoName = doc.getString("fullName") ?: "Unknown PCO"
                        val isApproved = status.equals("Approved", ignoreCase = true)

                        // PCO notification
                        NotificationManager.sendNotificationToUser(
                            receiverId = pcoUid,
                            title = if (isApproved) "Accreditation Approved" else "Accreditation Rejected",
                            message = if (isApproved)
                                "Your PCO Accreditation has been approved."
                            else
                                "Your PCO Accreditation has been rejected. Please review the feedback.",
                            category = "approval",
                            priority = "high",
                            module = "Accreditation",
                            documentId = id,
                            actionLink = "accreditationDashboard/$id" // deep link for PCO
                        )

                        // EMB notification
                        NotificationManager.sendNotificationToUser(
                            receiverId = embUid,
                            title = "Accreditation ${status.uppercase()}",
                            message = "You have $status a PCO accreditation application for $pcoName.",
                            category = "system",
                            priority = "high",
                            module = "Accreditation",
                            documentId = id,
                            actionLink = "accreditationEmbDashboard/$id" // deep link for EMB
                        )
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update status: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("PCO_REVIEW", "❌ Failed to update accreditation status", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
