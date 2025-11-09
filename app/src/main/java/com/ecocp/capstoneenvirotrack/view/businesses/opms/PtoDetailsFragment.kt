package com.ecocp.capstoneenvirotrack.view.businesses.opms

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.databinding.FragmentPtoDetailsBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PtoDetailsFragment : Fragment() {

    private lateinit var binding: FragmentPtoDetailsBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPtoDetailsBinding.inflate(inflater, container, false)

        val applicationId = arguments?.getString("applicationId")

        if (applicationId != null) {
            fetchPtoDetails(applicationId)
        } else {
            binding.txtOwnerName.text = "No application data found."
        }

        return binding.root
    }

    private fun fetchPtoDetails(applicationId: String) {
        db.collection("opms_pto_applications")
            .document(applicationId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    binding.txtOwnerName.text = doc.getString("ownerName") ?: "-"
                    binding.txtEstablishmentName.text = doc.getString("establishmentName") ?: "-"
                    binding.txtNatureOfBusiness.text = doc.getString("natureOfBusiness") ?: "-"
                    binding.txtTin.text = doc.getString("tin") ?: "-"
                    binding.txtOwnershipType.text = doc.getString("ownershipType") ?: "-"
                    binding.txtMailingAddress.text = doc.getString("mailingAddress") ?: "-"
                    binding.txtPlantAddress.text = doc.getString("plantAddress") ?: "-"

                    binding.txtPcoName.text = doc.getString("pcoName") ?: "-"
                    binding.txtPcoAccreditation.text = doc.getString("pcoAccreditation") ?: "-"
                    binding.txtOperatingHours.text = doc.getString("operatingHours") ?: "-"
                    binding.txtTotalEmployees.text = doc.getString("totalEmployees") ?: "-"
                    binding.txtLandArea.text = doc.getString("landArea") ?: "-"

                    binding.txtEquipmentName.text = doc.getString("equipmentName") ?: "-"
                    binding.txtFuelType.text = doc.getString("fuelType") ?: "-"
                    binding.txtEmissionsSummary.text = doc.getString("emissionsSummary") ?: "-"

                    val amount = doc.getDouble("amount") ?: 0.0
                    val currency = doc.getString("currency") ?: "PHP"
                    val paymentMethod = doc.getString("paymentMethod") ?: "-"
                    val paymentStatus = doc.getString("paymentStatus") ?: "Pending"
                    val paymentTimestamp = doc.getTimestamp("paymentTimestamp")?.toDate()
                    val formattedDate = paymentTimestamp?.let {
                        SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(it)
                    } ?: "-"

                    binding.txtPayment.text = "₱%.2f %s\nMethod: %s\nStatus: %s\nPaid on: %s".format(
                        amount, currency, paymentMethod, paymentStatus, formattedDate
                    )

                    // 🔹 Show feedback if available
                    val feedback = doc.getString("feedback") ?: ""
                    if (feedback.isNotBlank()) {
                        binding.inputFeedback.visibility = View.VISIBLE
                        binding.inputFeedback.setText(feedback)
                        binding.inputFeedback.isEnabled = false
                        binding.inputFeedback.setTextColor(resources.getColor(android.R.color.darker_gray))
                    } else {
                        binding.inputFeedback.visibility = View.GONE
                    }

                    binding.txtStatus.text = "Status: ${doc.getString("status") ?: "Pending"}"

                    // 🔹 Check if certificate exists, show download button
                    val certificateUrl = doc.getString("certificateUrl")
                    if (!certificateUrl.isNullOrBlank()) {
                        binding.btnDownloadCertificate.visibility = View.VISIBLE
                        binding.btnDownloadCertificate.setOnClickListener {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(certificateUrl))
                                startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } else {
                        binding.btnDownloadCertificate.visibility = View.GONE
                    }

                } else {
                    binding.txtOwnerName.text = "No details found for this PTO."
                }
            }
            .addOnFailureListener {
                binding.txtOwnerName.text = "Error loading PTO details."
            }
    }
}
