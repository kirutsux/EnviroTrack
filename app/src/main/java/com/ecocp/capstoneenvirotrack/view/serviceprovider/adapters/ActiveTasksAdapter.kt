package com.ecocp.capstoneenvirotrack.view.serviceprovider.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.TransporterBooking
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class ActiveTasksAdapter(
    private val bookings: MutableList<TransporterBooking>,
    private val onViewClick: (TransporterBooking) -> Unit
) : RecyclerView.Adapter<ActiveTasksAdapter.ActiveTaskViewHolder>() {

    inner class ActiveTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtServiceTitle: TextView = itemView.findViewById(R.id.txtServiceTitle)
        val txtCompanyName: TextView = itemView.findViewById(R.id.txtCompanyName)
        val txtBookingDate: TextView = itemView.findViewById(R.id.txtBookingDate)
        val bookingStatus: TextView = itemView.findViewById(R.id.bookingStatus)
        val btnView: Button = itemView.findViewById(R.id.btnView)
        val imgClient: ImageView = itemView.findViewById(R.id.imgClient)
    }

    // add near top of class
    private val activeStatuses = setOf("confirmed", "in transit", "delivered")

    private val confirmedList: List<TransporterBooking>
        get() = bookings.filter {
            val s = (it.bookingStatus ?: "").lowercase().trim()
            activeStatuses.contains(s)
        }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveTaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_active_task, parent, false)
        return ActiveTaskViewHolder(view)
    }

    override fun getItemCount(): Int {
        return confirmedList.size
    }

    override fun onBindViewHolder(holder: ActiveTaskViewHolder, position: Int) {
        val booking = confirmedList[position]

        holder.txtServiceTitle.text = booking.serviceProviderName
        holder.txtCompanyName.text = booking.serviceProviderCompany
        holder.bookingStatus.text = booking.bookingStatus

        // Convert Timestamp to readable date (handles Timestamp or String)
        val timestamp = booking.bookingDate
        holder.txtBookingDate.text = when (timestamp) {
            is Timestamp -> {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                sdf.format(timestamp.toDate())
            }
            is String -> timestamp
            null -> ""
            else -> timestamp.toString()
        }

        // Optional: Load client image if URL available (uncomment + add Glide dependency)
        // booking.clientImageUrl?.takeIf { it.isNotBlank() }?.let { Glide.with(holder.itemView).load(it).into(holder.imgClient) }

        holder.btnView.setOnClickListener {
            onViewClick(booking)
        }
    }

    /** Convenience: replace the backing list and refresh */
    fun replaceAll(newList: List<TransporterBooking>) {
        bookings.clear()
        bookings.addAll(newList)
        notifyDataSetChanged()
    }
}
