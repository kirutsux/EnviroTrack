package com.ecocp.capstoneenvirotrack.view.businesses.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.databinding.BottomsheetTransportDetailsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class TransportDetailsBottomSheet(
    private val transportBookingId: String
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

        disableButton(binding.btnProceedStep3)
        disableButton(binding.btnViewPTTCert)

        loadTransportBooking()
        checkForPTTCertificate()

        // Proceed to Step 3
        binding.btnProceedStep3.setOnClickListener {
            findNavController().navigate(
                TransportDetailsBottomSheetDirections
                    .actionTransportDetailsBottomSheetToTsdFacilitySelectionFragment(transportBookingId)
            )
        }

        // SEND PTT CERTIFICATE ONLY (NO NAVIGATION)
        binding.btnViewPTTCert.setOnClickListener {
            sendPTTCertificateToTransporter()
        }

        return binding.root
    }

    private fun loadTransportBooking() {
        db.collection("transport_bookings").document(transportBookingId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {

                    binding.tvTitle.text = doc.getString("wasteType") ?: "Transport Booking"
                    binding.tvQuantity.text = "Quantity: ${doc.getString("quantity") ?: ""}"
                    binding.tvTransporter.text = "Transporter: ${doc.getString("serviceProviderName") ?: ""}"
                    binding.tvOrigin.text = "Pickup Address: ${doc.getString("origin") ?: "N/A"}"
                    binding.tvBookingDate.text = "Booking Date: ${doc.getTimestamp("bookingDate")?.toDate() ?: "N/A"}"
                    binding.tvSpecialInstructions.text = "Instructions: ${doc.getString("specialInstructions") ?: ""}"

                    val bookingStatus = doc.getString("bookingStatus") ?: "Pending"
                    binding.tvStatus.text = "Booking Status: $bookingStatus"

                    if (bookingStatus == "Confirmed") enableButton(binding.btnProceedStep3)
                }
            }
    }

    private fun checkForPTTCertificate() {
        db.collection("ptt_applications")
            .whereEqualTo("transportBookingId", transportBookingId)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    val doc = snap.documents.first()
                    val cert = doc.getString("pttCertificate")

                    if (!cert.isNullOrEmpty()) {
                        enableButton(binding.btnViewPTTCert)
                    }
                }
            }
    }

    // ----------------------------------------------------
    // SAVE PTT CERTIFICATE FROM ptt_applications TO BOOKING
    // ----------------------------------------------------
    private fun sendPTTCertificateToTransporter() {

        db.collection("ptt_applications")
            .whereEqualTo("transportBookingId", transportBookingId)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    Toast.makeText(requireContext(), "No PTT Certificate found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val pttDoc = snap.documents.first()
                val pttCertValue = pttDoc.getString("pttCertificate") ?: ""

                if (pttCertValue.isEmpty()) {
                    Toast.makeText(requireContext(), "PTT Certificate is empty.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Save certificate to transport_bookings
                db.collection("transport_bookings")
                    .document(transportBookingId)
                    .set(mapOf("pttCertificate" to pttCertValue), SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "PTT Certificate successfully sent to transporter.", Toast.LENGTH_SHORT).show()
                        enableButton(binding.btnViewPTTCert)
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to send PTT certificate.", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun enableButton(button: View) {
        button.isEnabled = true
        button.alpha = 1f
    }

    private fun disableButton(button: View) {
        button.isEnabled = false
        button.alpha = 0.5f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
