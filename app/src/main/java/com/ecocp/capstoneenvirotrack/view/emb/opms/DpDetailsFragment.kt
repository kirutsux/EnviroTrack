package com.ecocp.capstoneenvirotrack.view.emb.opms

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
import com.ecocp.capstoneenvirotrack.databinding.FragmentDpDetailsBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DpDetailsFragment : Fragment() {

    private var _binding: FragmentDpDetailsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var applicationId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDpDetailsBinding.inflate(inflater, container, false)
        applicationId = arguments?.getString("applicationId")

        // ✅ Retrieve applicationId passed from dashboard
        applicationId = arguments?.getString("applicationId")
        if (applicationId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No Application ID provided", Toast.LENGTH_SHORT).show()
            Log.e("DP_DETAILS", "❌ applicationId is null or empty")
        } else {
            Log.d("DP_DETAILS", "🔍 Loading details for ID: $applicationId")
            loadDischargePermitDetails()
        }

        binding.btnApprove.setOnClickListener { updateStatus("Approved") }
        binding.btnReject.setOnClickListener { updateStatus("Rejected") }

        return binding.root
    }

    // ✅ Load Discharge Permit details
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

                    // ✅ Payment Info
                    val amount = doc.getDouble("amount") ?: 0.0
                    val currency = doc.getString("currency") ?: "PHP"
                    val paymentMethod = doc.getString("paymentMethod") ?: "-"
                    val paymentTimestamp = doc.getTimestamp("paymentTimestamp")
                    val submittedTimestamp = doc.getTimestamp("submittedTimestamp")

                    val paidOn = paymentTimestamp?.toDate()?.toString() ?: "-"
                    val submittedOn = submittedTimestamp?.toDate()?.toString() ?: "-"

                    txtAmount.text = "₱%.2f".format(amount)
                    txtPaymentMethod.text = "Method: $paymentMethod"
                    txtPaymentStatus.text = "Status: Paid"
                    txtPaymentTimestamp.text = "Paid on: $paidOn"
                    txtSubmittedTimestamp.text = "Submitted on: $submittedOn"

                    // 🔹Uploaded Files
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

    // ✅ Approve / Reject + Notifications
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
            "reviewedTimestamp" to Timestamp.now()
        )

        db.collection("opms_discharge_permits").document(id)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Application $status successfully!", Toast.LENGTH_SHORT).show()

                if (isAdded) {
                    val navController = requireActivity().findNavController(R.id.embopms_nav_host_fragment)
                    navController.popBackStack(R.id.opmsEmbDashboardFragment, false)
                }

                // ✅ Send Notifications to PCO & EMB
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
