package com.ecocp.capstoneenvirotrack.view.businesses.dialogs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.databinding.BottomsheetTsdBookingBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TsdBookingBottomSheet(
    private val data: Map<String, Any>
) : BottomSheetDialogFragment() {

    private var _binding: BottomsheetTsdBookingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetTsdBookingBinding.inflate(inflater, container, false)

        // Populate fields (unchanged)
        binding.tvTitle.text = data["tsdName"] as? String ?: "TSD Booking Details"
        binding.tvWasteType.text = "Waste Type: ${data["wasteType"] ?: "N/A"}"
        binding.tvTreatment.text = "Treatment Info: ${data["treatmentInfo"] ?: "N/A"}"
        binding.tvQuantity.text = "Quantity: ${data["quantity"] ?: 0}"
        binding.tvRate.text = "Rate: ${data["rate"] ?: 0}"
        binding.tvAmount.text = "Amount: ${data["amount"] ?: 0}"
        binding.tvPreferredDate.text = "Preferred Date: ${data["preferredDate"] ?: "N/A"}"
        binding.tvLocation.text = "Location: ${data["location"] ?: "N/A"}"
        binding.tvPaymentStatus.text = "Payment Status: ${data["paymentStatus"] ?: "Pending"}"
        binding.tvStatus.text = "Status: ${(data["status"] as? String)?.capitalize() ?: "Pending"}"

        // Certificate & Previous Record Links (unchanged)
        binding.tvCertificate.setOnClickListener {
            (data["certificateUrl"] as? String)?.let { url ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        binding.tvPreviousRecord.setOnClickListener {
            (data["previousRecordUrl"] as? String)?.let { url ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        // CORRECTED & IMPROVED: Allow "confirmed" OR "delivered"
        val bookingStatus = (data["bookingStatus"] as? String)?.lowercase()
        val isAllowed = bookingStatus == "confirmed" || bookingStatus == "delivered"

        binding.btnProceedStep3.apply {
            isEnabled = isAllowed
            alpha = if (isAllowed) 1.0f else 0.5f
            text = if (isAllowed) "Proceed to PTT Application" else "Awaiting Delivery"
        }

        binding.btnProceedStep3.setOnClickListener {
            if (isAllowed) {
                findNavController().navigate(com.ecocp.capstoneenvirotrack.R.id.pttApplicationFragment)
            } else {
                Toast.makeText(requireContext(), "This booking must be Delivered first", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
