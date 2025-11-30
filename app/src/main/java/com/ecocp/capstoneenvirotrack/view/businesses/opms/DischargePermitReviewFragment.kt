package com.ecocp.capstoneenvirotrack.view.businesses.opms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentDischargePermitReviewBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class DischargePermitReviewFragment : Fragment() {

    private var _binding: FragmentDischargePermitReviewBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private var currentDocId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDischargePermitReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchPermitDetails()

        binding.btnEditInfo.setOnClickListener {
            if (currentDocId != null) {
                val bundle = Bundle().apply {
                    putString("documentId", currentDocId) // Pass the document ID
                }
                findNavController().navigate(
                    R.id.action_dischargePermitReviewFragment_to_dischargePermitEditInfoFragment,
                    bundle
                )
            } else {
                Toast.makeText(requireContext(), "No application found to edit.", Toast.LENGTH_SHORT).show()
            }
        }


        binding.btnSubmitApplication.setOnClickListener {
            submitApplication()
        }

    }

    private fun fetchPermitDetails() {
        if (uid == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("opms_discharge_permits")
            .whereEqualTo("uid", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Toast.makeText(requireContext(), "No permit data found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val doc = result.documents.first()
                currentDocId = doc.id

                // --- Fetch all permit info ---
                val companyName = doc.getString("companyName") ?: "-"
                val companyAddress = doc.getString("companyAddress") ?: "-"
                val pcoName = doc.getString("pcoName") ?: "-"
                val pcoAccreditationNumber = doc.getString("pcoAccreditation") ?: "-"
                val contactNumber = doc.getString("contactNumber") ?: "-"
                val email = doc.getString("email") ?: "-"
                val receivingBody = doc.getString("bodyOfWater") ?: "-"
                val sourceWastewater = doc.getString("sourceWastewater") ?: "-"
                val dischargeVolume = doc.getString("volume") ?: "-"
                val dischargeMethod = doc.getString("treatmentMethod") ?: "-"
                val operationStartDate = doc.getString("operationStartDate") ?: "-"
                val uploadedFiles = doc.getString("fileLinks") ?: "No files uploaded"
                val status = doc.getString("status") ?: "Pending"

                // --- Fetch payment info (if exists) ---
                val amount = doc.getDouble("amount") ?: 0.0
                val currency = doc.getString("currency") ?: "PHP"
                val paymentMethod = doc.getString("paymentMethod") ?: "-"
                val paymentStatus = doc.getString("paymentStatus") ?: "Pending"
                val paymentTimestamp = doc.getTimestamp("paymentTimestamp")

                val formattedDate = paymentTimestamp?.toDate()?.let {
                    SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(it)
                } ?: "-"

                // --- Update UI ---
                binding.txtCompanyReview.text =
                    "$companyName\n$companyAddress\nPCO: $pcoName ($pcoAccreditationNumber)\n" +
                            "Contact: $contactNumber\nEmail: $email"

                binding.txtDischargeReview.text =
                    "Receiving Body: $receivingBody\nSource Wastewater: $sourceWastewater\n" +
                            "Volume: $dischargeVolume\nTreatment: $dischargeMethod\n" +
                            "Operation Start Date: $operationStartDate"

                // --- Payment Display ---
                val paymentDisplay = if (paymentStatus.equals("Paid", ignoreCase = true)) {
                    "✅ Payment Completed\nAmount: ₱$amount $currency\nMethod: $paymentMethod\nDate: $formattedDate"
                } else {
                    "❌ Payment Pending"
                }
                binding.txtPaymentReview.text = paymentDisplay
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to fetch data.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun submitApplication() {
        if (uid == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentDocId == null) {
            Toast.makeText(requireContext(), "No application found to update.", Toast.LENGTH_SHORT).show()
            return
        }

        val updateData = mapOf(
            "status" to "Pending",
            "submittedTimestamp" to Timestamp.now()
        )

        db.collection("opms_discharge_permits")
            .document(currentDocId!!)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Application submitted successfully!", Toast.LENGTH_SHORT).show()

                // ✅ Send notification to PCO (self)
                sendNotification(
                    receiverId = uid,
                    receiverType = "PCO",
                    title = "Discharge Permit Submission",
                    message = "You have successfully submitted a Discharge Permit application.",
                    type = "submission"
                )

                // ✅ Send notification to EMB (admin)
                // Replace with your actual EMB UID or set a specific receiverType = "EMB"
                db.collection("users")
                    .whereEqualTo("userType", "emb")
                    .get()
                    .addOnSuccessListener { embUsers ->
                        for (emb in embUsers) {
                            sendNotification(
                                receiverId = emb.id,
                                receiverType = "EMB",
                                title = "New Discharge Permit Application",
                                message = "A new Discharge Permit has been submitted by a PCO.",
                                type = "alert"
                            )
                        }
                    }


                // ✅ Navigate to OPMS Dashboard and clear back stack
                findNavController().navigate(
                    R.id.opmsDashboardFragment,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.opmsDashboardFragment, true)
                        .build()
                )
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to submit application.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun sendNotification(
        receiverId: String,
        receiverType: String,
        title: String,
        message: String,
        type: String
    ) {
        val notificationData = hashMapOf(
            "receiverId" to receiverId,
            "receiverType" to receiverType,
            "title" to title,
            "message" to message,
            "type" to type,
            "isRead" to false,
            "timestamp" to Timestamp.now()
        )

        db.collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                // Optional: Log or toast for debugging
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to send notification: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
