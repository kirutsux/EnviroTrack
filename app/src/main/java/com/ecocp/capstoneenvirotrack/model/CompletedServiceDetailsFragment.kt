package com.ecocp.capstoneenvirotrack.model

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.repository.BookingRepository
import com.ecocp.capstoneenvirotrack.viewmodel.BookingViewModel

class CompletedServiceDetailsFragment : Fragment(R.layout.fragment_booking_details) {
    private val viewModel: BookingViewModel by viewModels()
    private lateinit var role: BookingRepository.Role

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // You MUST pass this when navigating:
        // arguments.putString("role", "TSD")  or "TRANSPORTER"
        role = if (arguments?.getString("role") == "TSD")
            BookingRepository.Role.TSD
        else
            BookingRepository.Role.TRANSPORTER

        val tvBookingId: TextView = view.findViewById(R.id.tvBookingId)
        val tvActor: TextView = view.findViewById(R.id.tvActor)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvUpdatedAt: TextView = view.findViewById(R.id.tvUpdatedAt)
        val btnViewCertificate: Button = view.findViewById(R.id.btnViewCertificate)

        viewModel.booking.observe(viewLifecycleOwner) { model ->
            if (model == null) return@observe

            tvBookingId.text = model.bookingId
            tvActor.text = model.actorId
            tvStatus.text = model.status
            tvUpdatedAt.text = viewModel.formattedStatusDate(model)

            if (model.certificateUrl.isNullOrEmpty()) {
                btnViewCertificate.visibility = View.GONE
            } else {
                btnViewCertificate.visibility = View.VISIBLE
                btnViewCertificate.setOnClickListener {
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(model.certificateUrl))
                    startActivity(i)
                }
            }
        }

        val bookingId = arguments?.getString("bookingId") ?: ""
        viewModel.loadBooking(bookingId, role)
    }
}
