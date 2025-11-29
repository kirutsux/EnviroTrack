package com.ecocp.capstoneenvirotrack.view.serviceprovider.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.view.serviceprovider.TransporterBooking
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class ActiveTasksAdapter(
    private val bookings: List<TransporterBooking>,
    private val onViewClick: (TransporterBooking) -> Unit
) : RecyclerView.Adapter<ActiveTasksAdapter.ActiveTaskViewHolder>() {

    inner class ActiveTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtServiceTitle: TextView = itemView.findViewById(R.id.txtServiceTitle)
        val txtCompanyName: TextView = itemView.findViewById(R.id.txtCompanyName)
        val txtBookingDate: TextView = itemView.findViewById(R.id.txtBookingDate) // New
        val bookingStatus: TextView = itemView.findViewById(R.id.bookingStatus)
        val btnView: Button = itemView.findViewById(R.id.btnView)
        val imgClient: ImageView = itemView.findViewById(R.id.imgClient)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveTaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_active_task, parent, false)
        return ActiveTaskViewHolder(view)
    }

    override fun getItemCount(): Int {
        return bookings.count { it.bookingStatus == "Confirmed" }
    }

    override fun onBindViewHolder(holder: ActiveTaskViewHolder, position: Int) {
        val confirmedBookings = bookings.filter { it.bookingStatus == "Confirmed" }
        val booking = confirmedBookings[position]

        holder.txtServiceTitle.text = booking.serviceProviderName
        holder.txtCompanyName.text = booking.serviceProviderCompany
        holder.bookingStatus.text = booking.bookingStatus

        // Convert Timestamp to readable date
        val timestamp = booking.bookingDate
        holder.txtBookingDate.text = when (timestamp) {
            is Timestamp -> {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                sdf.format(timestamp.toDate())
            }
            is String -> timestamp
            else -> ""
        }

        // Optional: Load client image if URL available
        // Glide.with(holder.itemView).load(booking.clientImageUrl).into(holder.imgClient)

        // Handle VIEW click
        holder.btnView.setOnClickListener {
            onViewClick(booking)
        }
    }
}
