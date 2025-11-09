package com.ecocp.capstoneenvirotrack.view.businesses.cnc

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.databinding.FragmentCncDetailsBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CncDetailsFragment : Fragment() {

    private lateinit var binding: FragmentCncDetailsBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCncDetailsBinding.inflate(inflater, container, false)

        val applicationId = arguments?.getString("applicationId")
        if (applicationId != null) {
            fetchCncDetails(applicationId)
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

                // Section A
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

                // Section B
                binding.txtProjectType.text = doc.getString("projectType") ?: "-"
                binding.txtProjectScale.text = doc.getString("projectScale") ?: "-"
                binding.txtProjectCost.text = doc.getString("projectCost") ?: "-"
                binding.txtLandArea.text = doc.getString("landArea") ?: "-"
                binding.txtRawMaterials.text = doc.getString("rawMaterials") ?: "-"
                binding.txtProductionCapacity.text = doc.getString("productionCapacity") ?: "-"
                binding.txtUtilitiesUsed.text = doc.getString("utilitiesUsed") ?: "-"
                binding.txtWasteGenerated.text = doc.getString("wasteGenerated") ?: "-"

                // Section C
                binding.txtCoordinates.text = doc.getString("coordinates") ?: "-"
                binding.txtNearbyWaters.text = doc.getString("nearbyWaters") ?: "-"
                binding.txtResidentialProximity.text = doc.getString("residentialProximity") ?: "-"
                binding.txtEnvFeatures.text = doc.getString("envFeatures") ?: "-"
                binding.txtZoning.text = doc.getString("zoning") ?: "-"

                // Payment & status
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

                // Feedback
                val feedback = doc.getString("feedback") ?: ""
                if (feedback.isNotBlank()) {
                    binding.inputFeedback.visibility = View.VISIBLE
                    binding.inputFeedback.setText(feedback)
                    binding.inputFeedback.isEnabled = false
                    binding.inputFeedback.setTextColor(resources.getColor(android.R.color.darker_gray))
                } else {
                    binding.inputFeedback.visibility = View.GONE
                }

                // 🔹 Handle Certificate Download Button (NEW LOGIC)
                val status = doc.getString("status")?.lowercase(Locale.getDefault()) ?: "pending"
                val certificateUrl = doc.getString("certificateUrl")

                if (status == "approved" && !certificateUrl.isNullOrBlank()) {
                    // show download button only if approved + certificate exists
                    binding.btnDownloadCertificate.visibility = View.VISIBLE
                    binding.btnDownloadCertificate.setOnClickListener {
                        openFileLink(certificateUrl)
                    }
                } else {
                    // hide if rejected or no certificate
                    binding.btnDownloadCertificate.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error loading CNC details.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openFileLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to open file", Toast.LENGTH_SHORT).show()
        }
    }
}
