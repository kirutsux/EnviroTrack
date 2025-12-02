package com.ecocp.capstoneenvirotrack.view.businesses.opms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentPtoReviewBinding
import com.ecocp.capstoneenvirotrack.utils.NotificationManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class PtoReviewFragment : Fragment() {

    private var _binding: FragmentPtoReviewBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private var currentDocId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPtoReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchPtoDetails()

        binding.btnEditInfo.setOnClickListener {
            if (currentDocId != null) {
                val bundle = Bundle().apply {
                    putString("applicationId", currentDocId) // Pass the document ID only
                }
                findNavController().navigate(
                    R.id.action_ptoReviewFragment_to_ptoEditInfoFragment,
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

    private fun fetchPtoDetails() {
        if (uid == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("opms_pto_applications")
            .whereEqualTo("uid", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Toast.makeText(requireContext(), "No PTO data found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val doc = result.documents.first()
                currentDocId = doc.id

                // --- Company Info ---
                val ownerName = doc.getString("ownerName") ?: "-"
                val establishmentName = doc.getString("establishmentName") ?: "-"
                val mailingAddress = doc.getString("mailingAddress") ?: "-"
                val plantAddress = doc.getString("plantAddress") ?: "-"
                val tin = doc.getString("tin") ?: "-"
                val ownershipType = doc.getString("ownershipType") ?: "-"
                val natureOfBusiness = doc.getString("natureOfBusiness") ?: "-"

                // --- Facility Info ---
                val pcoName = doc.getString("pcoName") ?: "-"
                val pcoAccreditation = doc.getString("pcoAccreditation") ?: "-"
                val operatingHours = doc.getString("operatingHours") ?: "-"
                val totalEmployees = doc.getString("totalEmployees") ?: "-"
                val landArea = doc.getString("landArea") ?: "-"

                // --- Equipment Info ---
                val equipmentName = doc.getString("equipmentName") ?: "-"
                val fuelType = doc.getString("fuelType") ?: "-"
                val emissions = doc.getString("emissionsSummary") ?: "-"

                // --- Payment Info ---
                val amount = doc.getDouble("amount") ?: 0.0
                val paymentMethod = doc.getString("paymentMethod") ?: "-"
                val paymentStatus = doc.getString("paymentStatus") ?: "Pending"
                val paymentTimestamp = doc.getTimestamp("paymentTimestamp")

                val formattedDate = paymentTimestamp?.toDate()?.let {
                    SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(it)
                } ?: "-"

                // --- Bind to UI ---
                binding.txtCompanyReview.text =
                    "👤 Owner: $ownerName\n🏢 Establishment: $establishmentName\n🧾 TIN: $tin\n🏠 Ownership: $ownershipType\n🌿 Nature: $natureOfBusiness\n📬 Mailing: $mailingAddress\n🏭 Plant: $plantAddress"

                binding.txtFacilityReview.text =
                    "🧑‍🔬 PCO: $pcoName (Accreditation: $pcoAccreditation)\n⏱ Operating Hours: $operatingHours\n👥 Total Employees: $totalEmployees\n📏 Land Area: $landArea"

                binding.txtEquipmentReview.text =
                    "⚙ Equipment: $equipmentName\n⛽ Fuel Type: $fuelType\n💨 Emissions: $emissions"

                binding.txtPaymentReview.text = if (paymentStatus.equals("Paid", true)) {
                    "✅ Payment Completed\n💰 Amount: ₱$amount\n💳 Method: $paymentMethod\n📅 Date: $formattedDate"
                } else {
                    "❌ Payment Pending"
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to fetch PTO data.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun submitApplication() {
        if (uid == null || currentDocId == null) {
            Toast.makeText(requireContext(), "No application found to submit.", Toast.LENGTH_SHORT).show()
            return
        }

        val updateData = mapOf(
            "status" to "Pending", // EMB will review later
            "submittedTimestamp" to Timestamp.now()
        )

        db.collection("opms_pto_applications").document(currentDocId!!)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Application submitted successfully!", Toast.LENGTH_SHORT).show()

                // ----------------------------------------------------------------------
                // ✅ Notify PCO (self) - confirmation
                // ----------------------------------------------------------------------
                NotificationManager.sendNotificationToUser(
                    receiverId = uid!!,
                    title = "PTO Submitted",
                    message = "You have successfully submitted a Permit to Operate application.",
                    category = "submission",
                    priority = "medium",
                    module = "OPMS",
                    documentId = currentDocId!!
                )

                // ----------------------------------------------------------------------
                // ✅ Notify all EMB admins - alert
                // Exclude the submitting PCO UID to prevent duplicates
                // ----------------------------------------------------------------------
                NotificationManager.sendToAllEmb(
                    title = "New PTO Application",
                    message = "A new Permit to Operate has been submitted by a PCO.",
                    category = "alert",
                    priority = "high",
                    module = "OPMS",
                    documentId = currentDocId!!,
                    excludeUid = uid // <-- ensures PCO does not receive this notification
                )

                // ----------------------------------------------------------------------
                // ✅ Navigate back to OPMS Dashboard and clear back stack
                // ----------------------------------------------------------------------
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
