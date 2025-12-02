package com.ecocp.capstoneenvirotrack.view.emb.crs

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
import com.ecocp.capstoneenvirotrack.databinding.FragmentCrsReviewDetailsBinding
import com.ecocp.capstoneenvirotrack.utils.NotificationManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CrsReviewDetailsFragment : Fragment() {

    private var _binding: FragmentCrsReviewDetailsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private var applicationId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCrsReviewDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applicationId = arguments?.getString("applicationId")
        if (applicationId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Application ID missing", Toast.LENGTH_SHORT).show()
            return
        }

        loadCrsDetails(applicationId!!)

        binding.btnApprove.setOnClickListener { updateStatus("Approved") }
        binding.btnReject.setOnClickListener { updateStatus("Rejected") }
    }

    private fun loadCrsDetails(id: String) {
        db.collection("crs_applications").document(id)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(requireContext(), "Application not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // 🔹 Basic Info
                binding.txtCompanyName.text = doc.getString("companyName") ?: "-"
                binding.txtCompanyType.text = doc.getString("companyType") ?: "-"
                binding.txtNatureOfBusiness.text = doc.getString("natureOfBusiness") ?: "-"
                binding.txtIndustryDescriptor.text = doc.getString("industryDescriptor") ?: "-"
                binding.txtAddress.text = doc.getString("address") ?: "-"
                binding.txtTinNumber.text = doc.getString("tinNumber") ?: "-"

                // 🔹 CEO Details
                binding.txtCeoName.text = doc.getString("ceoName") ?: "-"
                binding.txtCeoContact.text = doc.getString("ceoContact") ?: "-"

                // 🔹 Representative Info
                val rep = doc.get("representative") as? Map<*, *>
                binding.txtRepName.text = rep?.get("name") as? String ?: "-"
                binding.txtRepEmail.text = rep?.get("email") as? String ?: "-"
                binding.txtRepContact.text = rep?.get("contact") as? String ?: "-"
                binding.txtRepPosition.text = rep?.get("position") as? String ?: "-"

                // 🔹 Contact Info
                val contact = doc.get("contactDetails") as? Map<*, *>
                binding.txtEmail.text = contact?.get("email") as? String ?: "-"
                binding.txtPhone.text = contact?.get("phone") as? String ?: "-"
                binding.txtWebsite.text = contact?.get("website") as? String ?: "-"

                // 🔹 File Links
                val fileUrls = doc.get("fileUrls") as? List<*>
                binding.linkFiles.removeAllViews()

                if (!fileUrls.isNullOrEmpty()) {
                    for ((index, url) in fileUrls.withIndex()) {
                        val linkView = TextView(requireContext()).apply {
                            text = "📎 File ${index + 1}"
                            setTextColor(resources.getColor(android.R.color.holo_blue_dark))
                            textSize = 15f
                            setPadding(8, 8, 8, 8)
                            setOnClickListener {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()))
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(requireContext(), "Cannot open file link", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        binding.linkFiles.addView(linkView)
                    }
                } else {
                    val noFilesView = TextView(requireContext()).apply {
                        text = "No files uploaded"
                        setPadding(8, 8, 8, 8)
                    }
                    binding.linkFiles.addView(noFilesView)
                }

                // 🔹 Date
                val dateSubmitted = doc.getTimestamp("dateSubmitted")
                val formattedDate = dateSubmitted?.toDate()?.let {
                    SimpleDateFormat("MMMM d, yyyy h:mm a", Locale.getDefault()).format(it)
                } ?: "Unknown"
                binding.txtDateSubmitted.text = "Submitted on: $formattedDate"

                // 🔹 Status and Feedback
                val status = doc.getString("status") ?: "Pending"
                val feedback = doc.getString("feedback") ?: ""

                binding.txtStatus.text = status
                when (status.lowercase(Locale.getDefault())) {
                    "approved", "rejected" -> {
                        binding.btnApprove.visibility = View.GONE
                        binding.btnReject.visibility = View.GONE
                        binding.inputFeedback.visibility = View.VISIBLE
                        binding.inputFeedback.setText(feedback.ifBlank { "No feedback provided." })
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
                Log.e("CRS EMB", "Error loading CRS details: ${e.message}")
                Toast.makeText(requireContext(), "Error loading CRS details.", Toast.LENGTH_SHORT).show()
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

        db.collection("crs_applications").document(id)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Application $status successfully!", Toast.LENGTH_SHORT).show()

                if (isAdded) {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }

                // ✅ Send notifications using NotificationManager
                db.collection("crs_applications").document(id).get()
                    .addOnSuccessListener { doc ->
                        val pcoUid = doc.getString("userId") ?: return@addOnSuccessListener
                        val companyName = doc.getString("companyName") ?: "Unknown Company"
                        val isApproved = status.equals("Approved", ignoreCase = true)

                        // PCO notification
                        NotificationManager.sendNotificationToUser(
                            receiverId = pcoUid,
                            title = if (isApproved) "Company Registration Approved" else "Company Registration Rejected",
                            message = if (isApproved)
                                "Your Company Registration has been approved."
                            else
                                "Your Company Registration has been rejected. Please review the feedback.",
                            category = "approval",
                            priority = "high",
                            module = "CRS",
                            documentId = id,
                            actionLink = "crsDashboard/$id" // deep link for PCO
                        )

                        // EMB notification
                        NotificationManager.sendNotificationToUser(
                            receiverId = embUid,
                            title = "Company Registration ${status.uppercase()}",
                            message = "You have $status a company registration application for $companyName.",
                            category = "system",
                            priority = "high",
                            module = "CRS",
                            documentId = id,
                            actionLink = "crsEmbDashboard/$id" // deep link for EMB
                        )
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update status: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("CRS_REVIEW", "❌ Failed to update CRS application status", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
