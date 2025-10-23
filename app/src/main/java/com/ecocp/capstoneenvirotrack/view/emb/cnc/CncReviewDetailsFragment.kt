package com.ecocp.capstoneenvirotrack.view.emb.cnc

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
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.databinding.FragmentCncReviewDetailsBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import com.ecocp.capstoneenvirotrack.R

class CncReviewDetailsFragment : Fragment() {

    private var _binding: FragmentCncReviewDetailsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var applicationId: String? = null

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
                        binding.btnApprove.visibility = View.GONE
                        binding.btnReject.visibility = View.GONE
                        binding.inputFeedback.visibility = View.GONE
                    }
                    "rejected" -> {
                        binding.btnApprove.visibility = View.GONE
                        binding.btnReject.visibility = View.GONE

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

    // 🔹 Show clickable file links
    private fun displayFileLinks(fileLinks: List<String>) {
        binding.layoutFileLinks.removeAllViews()

        for ((index, link) in fileLinks.withIndex()) {
            val textView = TextView(requireContext()).apply {
                // Try to extract filename
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

    private fun setupButtons() {
        binding.btnApprove.setOnClickListener {
            updateStatus("Approved", "Application approved by EMB.")
        }

        binding.btnReject.setOnClickListener {
            val feedback = binding.inputFeedback.text.toString().trim()
            if (feedback.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter feedback before rejecting.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            updateStatus("Rejected", feedback)
        }
    }

    private fun updateStatus(status: String, feedback: String) {
        val id = applicationId ?: return
        val embUid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val updateData = mapOf(
            "status" to status,
            "feedback" to feedback,
            "reviewedTimestamp" to Timestamp.now()
        )

        // 1️⃣ Update CNC application status
        db.collection("cnc_applications").document(id)
            .update(updateData)
            .addOnSuccessListener {
                // ✅ Toast + immediate navigation
                Toast.makeText(requireContext(), "Application $status successfully!", Toast.LENGTH_SHORT).show()
                if (isAdded) {
                    val navController = requireActivity().findNavController(R.id.embcnc_nav_host_fragment)
                    navController.popBackStack(R.id.cncEmbDashboardFragment, false)
                }

                // 2️⃣ Fire off notifications asynchronously
                db.collection("cnc_applications").document(id).get()
                    .addOnSuccessListener { doc ->
                        val pcoId = doc.getString("uid") ?: run {
                            Log.e("CNC_REVIEW", "❌ PCO ID not found for notification")
                            return@addOnSuccessListener
                        }
                        val companyName = doc.getString("companyName") ?: "Unknown Company"
                        val isApproved = status.equals("Approved", ignoreCase = true)

                        val notificationForPCO = hashMapOf(
                            "receiverId" to pcoId,
                            "receiverType" to "pco",
                            "senderId" to embUid,
                            "title" to if (isApproved) "Application Approved" else "Application Rejected",
                            "message" to if (isApproved)
                                "Your CNC application has been approved."
                            else
                                "Your CNC application has been rejected. Please review the feedback.",
                            "timestamp" to Timestamp.now(),
                            "isRead" to false,
                            "applicationId" to id
                        )

                        val notificationForEMB = hashMapOf(
                            "receiverId" to embUid,
                            "receiverType" to "emb",
                            "senderId" to embUid,
                            "title" to "CNC Application ${status.uppercase()}",
                            "message" to "You have $status a CNC application by $companyName.",
                            "timestamp" to Timestamp.now(),
                            "isRead" to false,
                            "applicationId" to id
                        )

                        // ✅ Add notifications individually with proper logging
                        db.collection("notifications").add(notificationForPCO)
                            .addOnSuccessListener { Log.d("CNC_REVIEW", "✅ Notification sent to PCO") }
                            .addOnFailureListener { e -> Log.e("CNC_REVIEW", "❌ Failed to notify PCO", e) }

                        db.collection("notifications").add(notificationForEMB)
                            .addOnSuccessListener { Log.d("CNC_REVIEW", "✅ Notification sent to EMB") }
                            .addOnFailureListener { e -> Log.e("CNC_REVIEW", "❌ Failed to notify EMB", e) }
                    }
                    .addOnFailureListener { e ->
                        Log.e("CNC_REVIEW", "❌ Failed to fetch CNC document for notifications", e)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update status: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("CNC_REVIEW", "❌ Failed to update CNC status", e)
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
