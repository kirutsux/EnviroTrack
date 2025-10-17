package com.ecocp.capstoneenvirotrack.view.businesses.cnc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentCncReviewBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class CncReviewFragment : Fragment() {

    private var _binding: FragmentCncReviewBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private var currentDocId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCncReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchCncDetails()

        binding.btnCncEditInfo.setOnClickListener {
            findNavController().popBackStack(R.id.cncFormFragment, false)
        }

        binding.btnCncSubmitApplication.setOnClickListener {
            submitCncApplication()
        }
    }

    private fun fetchCncDetails() {
        if (uid == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("cnc_applications")
            .whereEqualTo("uid", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Toast.makeText(requireContext(), "No CNC data found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val doc = result.documents.first()
                currentDocId = doc.id

                // --- Section A ---
                val companyName = doc.getString("companyName") ?: "-"
                val businessName = doc.getString("businessName") ?: "-"
                val projectTitle = doc.getString("projectTitle") ?: "-"
                val natureOfBusiness = doc.getString("natureOfBusiness") ?: "-"
                val projectLocation = doc.getString("projectLocation") ?: "-"
                val email = doc.getString("email") ?: "-"
                val managingHead = doc.getString("managingHead") ?: "-"
                val pcoName = doc.getString("pcoName") ?: "-"
                val pcoAccreditation = doc.getString("pcoAccreditation") ?: "-"
                val dateEstablished = doc.getString("dateEstablished") ?: "-"
                val numEmployees = doc.getString("numEmployees") ?: "-"
                val psicCode = doc.getString("psicCode") ?: "-"

                // --- Section B ---
                val projectType = doc.getString("projectType") ?: "-"
                val projectScale = doc.getString("projectScale") ?: "-"
                val projectCost = doc.getString("projectCost") ?: "-"
                val landArea = doc.getString("landArea") ?: "-"
                val rawMaterials = doc.getString("rawMaterials") ?: "-"
                val productionCapacity = doc.getString("productionCapacity") ?: "-"
                val utilitiesUsed = doc.getString("utilitiesUsed") ?: "-"
                val wasteGenerated = doc.getString("wasteGenerated") ?: "-"

                // --- Section C ---
                val coordinates = doc.getString("coordinates") ?: "-"
                val nearbyWaters = doc.getString("nearbyWaters") ?: "-"
                val residentialProximity = doc.getString("residentialProximity") ?: "-"
                val envFeatures = doc.getString("envFeatures") ?: "-"
                val zoning = doc.getString("zoning") ?: "-"

                // --- Payment Info ---
                val amount = doc.getDouble("amount") ?: 0.0
                val paymentMethod = doc.getString("paymentMethod") ?: "-"
                val paymentStatus = doc.getString("paymentStatus") ?: "Pending"
                val paymentTimestamp = doc.getTimestamp("paymentTimestamp")

                val formattedDate = paymentTimestamp?.toDate()?.let {
                    SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(it)
                } ?: "-"

                // === Bind to UI ===
                binding.txtCncCompanyReview.text =
                    "🏢 Company: $companyName\n💼 Business: $businessName\n📄 Project Title: $projectTitle\n🌿 Nature of Business: $natureOfBusiness\n📍 Project Location: $projectLocation\n📧 Email: $email\n👤 Managing Head: $managingHead\n🧑‍🔬 PCO: $pcoName (Accreditation: $pcoAccreditation)\n📅 Established: $dateEstablished\n👥 Employees: $numEmployees\n🧾 PSIC Code: $psicCode"

                binding.txtCncProjectReview.text =
                    "🏗 Project Type: $projectType\n📏 Project Scale: $projectScale\n💰 Project Cost: $projectCost\n🌳 Land Area: $landArea\n⚙ Raw Materials: $rawMaterials\n🏭 Production Capacity: $productionCapacity\n💧 Utilities Used: $utilitiesUsed\n🗑 Waste Generated: $wasteGenerated"

                binding.txtCncEnvironmentalReview.text =
                    "📍 Coordinates: $coordinates\n🌊 Nearby Water Bodies: $nearbyWaters\n🏠 Residential Proximity: $residentialProximity\n🌾 Environmental Features: $envFeatures\n🗺 Zoning Classification: $zoning"

                binding.txtCncPaymentReview.text = if (paymentStatus.equals("Paid", true)) {
                    "✅ Payment Completed\n💰 Amount: ₱$amount\n💳 Method: $paymentMethod\n📅 Date: $formattedDate"
                } else {
                    "❌ Payment Pending"
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to fetch CNC data.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun submitCncApplication() {
        if (uid == null || currentDocId == null) {
            Toast.makeText(requireContext(), "No application found to submit.", Toast.LENGTH_SHORT).show()
            return
        }

        val updateData = mapOf(
            "status" to "Pending",
            "submittedTimestamp" to Timestamp.now()
        )

        db.collection("cnc_applications").document(currentDocId!!)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "CNC Application submitted successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.cncDashboardFragment)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to submit CNC application.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
