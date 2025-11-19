package com.ecocp.capstoneenvirotrack.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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

        val etQuickMessage: EditText? = view.findViewById(R.id.etQuickMessage)
        val btnSendQuick: ImageButton? = view.findViewById(R.id.btnSendQuick)
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

        // Quick chat function
        holder.btnSendQuick?.setOnClickListener {
            val message = holder.etQuickMessage?.text.toString().trim()
            if (message.isNotEmpty()) {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
                val receiverId = booking.generatorId ?: return@setOnClickListener
                val receiverName = "Generator"

                val chatId = listOf(currentUserId, receiverId).sorted().joinToString("_")

                val messageObj = mapOf(
                    "senderId" to currentUserId,
                    "receiverId" to receiverId,
                    "message" to message,
                    "timestamp" to System.currentTimeMillis()
                )

                FirebaseDatabase.getInstance().getReference("Chats")
                    .child(chatId)
                    .push()
                    .setValue(messageObj)

                holder.etQuickMessage?.text?.clear()

                val navController = Navigation.findNavController(holder.itemView)
                val bundle = Bundle().apply {
                    putString("receiverId", receiverId)
                    putString("receiverName", receiverName)
                }
                navController.navigate(R.id.action_SP_Bookings_to_SP_ChatFragment, bundle)
            }
        }
    }

    override fun getItemCount(): Int = bookings.size
}
