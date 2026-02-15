package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.fragment.app.viewModels
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentBookingDetailsBinding
import com.ecocp.capstoneenvirotrack.repository.BookingRepository
import com.ecocp.capstoneenvirotrack.viewmodel.BookingViewModel

class Booking_details : Fragment(R.layout.fragment_booking_details) {

    private var _binding: FragmentBookingDetailsBinding? = null
    private val binding get() = _binding!!

    // ViewModel from the viewmodel package
    private val viewModel: BookingViewModel by viewModels()

    companion object {
        @JvmStatic
        fun newInstance(bookingId: String, role: String) =
            Booking_details().apply {
                arguments = Bundle().apply {
                    putString("bookingId", bookingId)
                    putString("role", role)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBookingDetailsBinding.bind(view)

        val bookingId = arguments?.getString("bookingId") ?: ""
        val roleArg = arguments?.getString("role") ?: "TRANSPORTER"
        val role = if (roleArg == "TSD") BookingRepository.Role.TSD else BookingRepository.Role.TRANSPORTER

        if (bookingId.isNotEmpty()) viewModel.loadBooking(bookingId, role)

        viewModel.booking.observe(viewLifecycleOwner) { model ->
            if (model == null) {
                binding.tvBookingId.text = ""
                binding.tvActor.text = ""
                binding.tvStatus.text = "Not found"
                binding.tvUpdatedAt.text = ""
                binding.btnViewCertificate.visibility = View.GONE
                return@observe
            }

            binding.tvBookingId.text = model.bookingId
            binding.tvActor.text = model.actorId
            binding.tvStatus.text = model.status
            binding.tvUpdatedAt.text = viewModel.formattedStatusDate(model)

            if (model.certificateUrl.isNullOrEmpty()) {
                binding.btnViewCertificate.visibility = View.GONE
            } else {
                binding.btnViewCertificate.visibility = View.VISIBLE
                binding.btnViewCertificate.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(model.certificateUrl))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    requireContext().startActivity(intent)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
