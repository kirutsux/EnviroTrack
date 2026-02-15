package com.ecocp.capstoneenvirotrack.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.databinding.ItemHwmsWasteBinding
import com.ecocp.capstoneenvirotrack.model.WasteGenDisplay
import java.text.SimpleDateFormat
import java.util.*

class Tab1WasteGenAdapter(
    private var items: MutableList<WasteGenDisplay>,
    private val onClick: (WasteGenDisplay) -> Unit
) : RecyclerView.Adapter<Tab1WasteGenAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemHwmsWasteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WasteGenDisplay) = with(binding) {

            tvCompanyName.text = item.companyName
            tvEmbNo.text = "EMB Reg No: ${item.embRegNo}"

            // Format timestamp
            val dateFormatted = item.timestamp?.toDate()?.let {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
            } ?: "N/A"
            tvDate.text = "Submitted: $dateFormatted"

            // Waste list
            containerWasteList.removeAllViews()
            item.wasteList.forEach { waste ->
                val tv = TextView(root.context).apply {
                    text = "• ${waste.wasteName} — ${waste.quantity} (${waste.wasteCode})"
                    textSize = 14f
                    setTextColor(Color.parseColor("#333333"))
                    setPadding(0, 2, 0, 2)
                }
                containerWasteList.addView(tv)
            }

            // Status color for Waste Gen
            val statusColor = when(item.status.lowercase()) {
                "draft" -> Color.GRAY
                "submitted" -> Color.parseColor("#FFA500")
                "confirmed" -> Color.parseColor("#2E7D32")
                "rejected" -> Color.RED
                else -> Color.DKGRAY
            }
            tvStatus.setTextColor(statusColor)
            tvStatus.text = "Status: ${item.status}"

            // Transport Badge
            if (!item.transportBookingId.isNullOrEmpty()) {

                // ✅ Normalize the status for comparison
                val normalizedStatus = item.transportStatus?.lowercase() ?: "loading"

                // Debug log
                android.util.Log.d("Tab1Adapter",
                    "Item ${item.id}: bookingId=${item.transportBookingId}, status=$normalizedStatus")

                val badge = TextView(root.context).apply {
                    // Display with proper capitalization
                    val displayText = when(normalizedStatus) {
                        "confirmed" -> "Confirmed"
                        "pending" -> "Pending"
                        "waiting" -> "Waiting"
                        "loading" -> "Loading..."
                        else -> normalizedStatus.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                        }
                    }
                    text = "Transport: $displayText"
                    textSize = 12f
                    setTextColor(Color.WHITE)

                    // Set background color based on status
                    val bgColor = when(normalizedStatus) {
                        "confirmed" -> Color.parseColor("#2E7D32")  // Green
                        "pending" -> Color.parseColor("#FFA500")     // Orange
                        "waiting" -> Color.GRAY
                        "loading" -> Color.parseColor("#607D8B")     // Blue-gray
                        else -> Color.GRAY
                    }
                    setBackgroundColor(bgColor)
                    setPadding(12, 6, 12, 6)
                }

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8 }

                containerWasteList.addView(badge, params)
            }

            // ✅ Click only if transport is confirmed
            val isClickable = item.transportStatus?.lowercase() == "confirmed"
            root.isClickable = isClickable
            root.alpha = if (isClickable) 1f else 0.6f

            root.setOnClickListener {
                if (isClickable) {
                    onClick(item)
                } else {
                    val statusMsg = when(item.transportStatus?.lowercase()) {
                        "pending" -> "Transport booking is pending confirmation."
                        "waiting" -> "Waiting for transporter confirmation."
                        "loading", null -> "Loading transport status..."
                        else -> "Transport not confirmed yet."
                    }
                    Toast.makeText(root.context, statusMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemHwmsWasteBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<WasteGenDisplay>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun updateTransportStatusForBooking(bookingId: String, status: String) {
        val index = items.indexOfFirst { it.transportBookingId == bookingId }
        if (index != -1) {
            android.util.Log.d("Tab1Adapter",
                "Updating booking $bookingId to status: $status")
            items[index].transportStatus = status
            notifyItemChanged(index)
        } else {
            android.util.Log.w("Tab1Adapter",
                "Booking $bookingId not found in items list")
        }
    }
}