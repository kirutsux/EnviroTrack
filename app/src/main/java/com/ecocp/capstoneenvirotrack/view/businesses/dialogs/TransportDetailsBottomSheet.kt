package com.ecocp.capstoneenvirotrack.view.businesses.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.BottomsheetTransportDetailsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore

class TransportDetailsBottomSheet(
    private val transportBookingId: String // pass the Firestore document ID
) : BottomSheetDialogFragment() {

    private var _binding: BottomsheetTransportDetailsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetTransportDetailsBinding.inflate(inflater, container, false)

        // Initially disable button
        binding.btnProceedStep3.isEnabled = false
        binding.btnProceedStep3.alpha = 0.5f

        // Fetch booking data from Firestore
        db.collection("transport_bookings").document(transportBookingId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val wasteType = doc.getString("wasteType") ?: "Transport Booking"
                    val quantity = doc.getString("quantity") ?: ""
                    val transporter = doc.getString("serviceProviderName") ?: ""
                    val origin = doc.getString("origin") ?: "N/A"
                    val bookingDate = doc.getTimestamp("bookingDate")?.toDate()?.toString() ?: "N/A"
                    val instructions = doc.getString("specialInstructions") ?: ""
                    val bookingStatus = doc.getString("bookingStatus") ?: "Pending"

                    // Populate fields
                    binding.tvTitle.text = wasteType
                    binding.tvQuantity.text = "Quantity: $quantity"
                    binding.tvTransporter.text = "Transporter: $transporter"
                    binding.tvOrigin.text = "Pickup Address: $origin"
                    binding.tvBookingDate.text = "Booking Date: $bookingDate"
                    binding.tvSpecialInstructions.text = "Instructions: $instructions"
                    binding.tvStatus.text = "Booking Status: $bookingStatus"

                    // Enable Proceed button only if bookingStatus is "Confirmed"
                    if (bookingStatus == "Confirmed") {
                        binding.btnProceedStep3.isEnabled = true
                        binding.btnProceedStep3.alpha = 1.0f
                    }
                }
            }
            .addOnFailureListener {
                binding.tvStatus.text = "Failed to load booking"
            }



        // Button click
        binding.btnProceedStep3.setOnClickListener {
            // Navigate to Step 3 fragment (replace R.id.hwmsStep3Fragment with your actual ID)
            findNavController().navigate(R.id.tsdFacilitySelectionFragment)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
