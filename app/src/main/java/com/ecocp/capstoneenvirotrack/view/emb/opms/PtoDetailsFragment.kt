package com.ecocp.capstoneenvirotrack.view.emb.opms

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentPtoDetails2Binding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PtoDetailsFragment : Fragment() {

    private var _binding: FragmentPtoDetails2Binding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private var applicationId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPtoDetails2Binding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()

        // ✅ Retrieve applicationId passed from dashboard
        applicationId = arguments?.getString("applicationId")
        if (applicationId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No Application ID provided", Toast.LENGTH_SHORT).show()
            Log.e("PTO_DETAILS", "❌ applicationId is null or empty")
        } else {
            Log.d("PTO_DETAILS", "🔍 Loading details for ID: $applicationId")
            loadApplicationDetails()
        }

        // ✅ Approve / Reject button listeners
        binding.btnApprove.setOnClickListener { updateStatus("Approved") }
        binding.btnReject.setOnClickListener { updateStatus("Rejected") }

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
                if (!isAdded || _binding == null) return@addOnSuccessListener // 🧱 Prevent crash

                if (doc.exists()) {
                    Log.d("PTO_DETAILS", "✅ Document data: ${doc.data}")

                    val amount = doc.getDouble("amount") ?: 0.0
                    val currency = doc.getString("currency") ?: "PHP"
                    val paymentMethod = doc.getString("paymentMethod") ?: "-"
                    val paymentStatus = doc.getString("paymentStatus") ?: "Pending"
                    val paymentTimestamp = doc.getTimestamp("paymentTimestamp")
                    val submittedTimestamp = doc.getTimestamp("submittedTimestamp")

                    val formattedPaymentDate = paymentTimestamp?.toDate()?.let {
                        SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(it)
                    } ?: "-"

                    val formattedSubmittedDate = submittedTimestamp?.toDate()?.let {
                        SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(it)
                    } ?: "-"

                    // ✅ Safely update binding only if fragment is still active
                    binding.apply {
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

                        // ✅ Update Payment Info Section
                        txtAmount.text = "₱%.2f %s".format(amount, currency)
                        txtPaymentMethod.text = "Method: $paymentMethod"
                        txtPaymentStatus.text = "Status: $paymentStatus"
                        txtPaymentTimestamp.text = "Paid on: $formattedPaymentDate"
                        txtSubmittedTimestamp.text = "Submitted on: $formattedSubmittedDate"

                        txtStatus.text = "Status: ${doc.getString("status") ?: "Pending"}"
                    }

                    Log.d("PTO_DETAILS", "💰 Payment updated: ₱$amount $currency, Method: $paymentMethod, Status: $paymentStatus, Paid on: $formattedPaymentDate")
                } else {
                    if (isAdded && _binding != null)
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
    // UPDATE STATUS (APPROVE / REJECT)
    // ------------------------------------------------------------
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

        db.collection("opms_pto_applications").document(id)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Application $status successfully!", Toast.LENGTH_SHORT).show()

                if (isAdded) {
                    val navController = requireActivity().findNavController(R.id.embopms_nav_host_fragment)
                    navController.popBackStack(R.id.opmsEmbDashboardFragment, false)
                }

                // ✅ Send Notifications to PCO and EMB
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
                                "Your Permit to Operate application has been approved."
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

    // ------------------------------------------------------------
    // CLEANUP BINDING
    // ------------------------------------------------------------
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
