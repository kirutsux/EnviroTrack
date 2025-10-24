package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.Booking
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(private val bookings: List<Booking>) :
    RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    inner class BookingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCompany: TextView = view.findViewById(R.id.tvCompany)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvWasteType: TextView = view.findViewById(R.id.tvWasteType)
        val tvOrigin: TextView = view.findViewById(R.id.tvOrigin)
        val tvDestination: TextView = view.findViewById(R.id.tvDestination)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val tvDateBooked: TextView = view.findViewById(R.id.tvDateBooked)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]

        holder.tvCompany.text = booking.serviceProviderCompany ?: "N/A"
        holder.tvStatus.text = booking.status ?: "Unknown"
        holder.tvWasteType.text = "Waste Type: ${booking.wasteType ?: "N/A"}"
        holder.tvOrigin.text = "Origin: ${booking.origin ?: "N/A"}"
        holder.tvDestination.text = "Destination: ${booking.destination ?: "N/A"}"
        holder.tvQuantity.text = "Quantity: ${booking.quantity ?: "N/A"}"

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = booking.dateBooked?.toDate()
        holder.tvDateBooked.text = "Date Booked: ${date?.let { dateFormat.format(it) } ?: "N/A"}"
    }

    override fun getItemCount(): Int = bookings.size
}
