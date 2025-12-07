package com.ecocp.capstoneenvirotrack.view.businesses.opms

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.api.PcoSendNotificationRequest
import com.ecocp.capstoneenvirotrack.api.RetrofitClient
import com.ecocp.capstoneenvirotrack.databinding.FragmentDischargePermitReviewBinding
import com.ecocp.capstoneenvirotrack.utils.NotificationManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.json.JSONObject
import retrofit2.Call
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val docId = currentDocId ?: run {
            Toast.makeText(requireContext(), "No application found to submit.", Toast.LENGTH_SHORT).show()
            return
        }

        val updateData = mapOf(
            "status" to "Pending",
            "submittedTimestamp" to Timestamp.now()
        )

        db.collection("opms_discharge_permits").document(docId)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Application submitted successfully!", Toast.LENGTH_SHORT).show()

                // --------------------------------------------------------
                // 🔔 CALL BACKEND API — NOTIFY PCO + ALL EMB USERS
                // --------------------------------------------------------
                val request = PcoSendNotificationRequest(
                    receiverId = uid,      // PCO UID
                    module = "DISCHARGE",  // Module name
                    documentId = docId
                )

                RetrofitClient.instance.sendPcoSubmissionNotification(request)
                    .enqueue(object : retrofit2.Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: retrofit2.Response<Void>) {
                            if (response.isSuccessful) {
                                Log.d("NOTIF", "Discharge submission notifications sent.")
                            } else {
                                Log.e("NOTIF", "Notification error: ${response.code()}")
                                Toast.makeText(requireContext(),
                                    "Notification failed: ${response.code()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            Log.e("NOTIF", "Notification error: ${t.message}")
                            Toast.makeText(requireContext(),
                                "Failed to send notifications",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })

                // --------------------------------------------------------
                // ✅ Navigate back to dashboard
                // --------------------------------------------------------
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




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
