package com.ecocp.capstoneenvirotrack.view.businesses.opms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.databinding.FragmentDischargePermitDetailsBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DischargePermitDetailsFragment : Fragment() {

    private lateinit var binding: FragmentDischargePermitDetailsBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDischargePermitDetailsBinding.inflate(inflater, container, false)

        val applicationId = arguments?.getString("applicationId")

        if (applicationId != null) {
            fetchPermitDetails(applicationId)
        } else {
            populateUIFromArgs()
        }

        return binding.root
    }

    private fun fetchPermitDetails(applicationId: String) {
        db.collection("opms_discharge_permits")
            .document(applicationId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    binding.txtCompanyName.text = doc.getString("companyName") ?: "-"
                    binding.txtCompanyAddress.text = doc.getString("companyAddress") ?: "-"
                    binding.txtPcoName.text = doc.getString("pcoName") ?: "-"
                    binding.txtAccreditation.text = doc.getString("pcoAccreditation") ?: "-"
                    binding.txtContactNumber.text = doc.getString("contactNumber") ?: "-"
                    binding.txtEmail.text = doc.getString("email") ?: "-"
                    binding.txtBodyOfWater.text = doc.getString("bodyOfWater") ?: "-"
                    binding.txtSourceWastewater.text = doc.getString("sourceWastewater") ?: "-"
                    binding.txtVolume.text = doc.getString("volume") ?: "-"
                    binding.txtTreatment.text = doc.getString("treatmentMethod") ?: "-"
                    binding.txtOperationDate.text = doc.getString("operationStartDate") ?: "-"
                    binding.txtStatus.text = doc.getString("status") ?: "-"

                    // ✅ Format payment info
                    val amount = doc.getDouble("amount") ?: 0.0
                    val currency = doc.getString("currency") ?: "PHP"
                    val paymentTimestamp = doc.getTimestamp("paymentTimestamp")?.toDate()

                    val formattedDate = paymentTimestamp?.let {
                        SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(it)
                    } ?: "Not Available"

                    binding.txtPayment.text =
                        "₱%.2f %s\nPaid on: %s".format(amount, currency, formattedDate)

                } else {
                    binding.txtCompanyName.text = "No details found"
                }
            }
            .addOnFailureListener {
                binding.txtCompanyName.text = "Error loading details"
            }
    }

    private fun populateUIFromArgs() {
        binding.txtCompanyName.text = arguments?.getString("companyName") ?: "-"
        binding.txtCompanyAddress.text = arguments?.getString("companyAddress") ?: "-"
        binding.txtPcoName.text = arguments?.getString("pcoName") ?: "-"
        binding.txtAccreditation.text = arguments?.getString("pcoAccreditation") ?: "-"
        binding.txtContactNumber.text = arguments?.getString("contactNumber") ?: "-"
        binding.txtEmail.text = arguments?.getString("email") ?: "-"
        binding.txtBodyOfWater.text = arguments?.getString("bodyOfWater") ?: "-"
        binding.txtSourceWastewater.text = arguments?.getString("sourceWastewater") ?: "-"
        binding.txtVolume.text = arguments?.getString("volume") ?: "-"
        binding.txtTreatment.text = arguments?.getString("treatmentMethod") ?: "-"
        binding.txtOperationDate.text = arguments?.getString("operationStartDate") ?: "-"
        binding.txtStatus.text = arguments?.getString("status") ?: "-"
        binding.txtPayment.text = arguments?.getString("paymentInfo") ?: "₱1,500 - Pending"
    }
}
