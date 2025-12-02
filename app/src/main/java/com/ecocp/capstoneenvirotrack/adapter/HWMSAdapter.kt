package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.databinding.ItemHwmsBinding
import com.ecocp.capstoneenvirotrack.model.DisplayItem

class HWMSAdapter(
    private var items: MutableList<DisplayItem>,
    private val onItemClick: (DisplayItem) -> Unit
) : RecyclerView.Adapter<HWMSAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemHwmsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DisplayItem) {
            binding.apply {

                tvTitle.text = item.title
                tvSubtitle.text = item.subtitle

                tvTransporter.text = item.transporter.ifEmpty { "—" }
                tvTsdFacility.text = item.tsdFacility.ifEmpty { "—" }
                tvPermitNo.text = item.permitNo.ifEmpty { "—" }

                tvPaymentStatus.text = "Payment: ${item.paymentStatus}"
                tvStatus.text = item.status

                // Colors
                when (item.paymentStatus.lowercase()) {
                    "paid" -> tvPaymentStatus.setTextColor(
                        ContextCompat.getColor(root.context, android.R.color.holo_green_dark)
                    )
                    "unpaid" -> tvPaymentStatus.setTextColor(
                        ContextCompat.getColor(root.context, android.R.color.holo_red_dark)
                    )
                    else -> tvPaymentStatus.setTextColor(
                        ContextCompat.getColor(root.context, android.R.color.darker_gray)
                    )
                }

                when (item.status.lowercase()) {
                    "pending" -> tvStatus.setTextColor(
                        ContextCompat.getColor(root.context, android.R.color.holo_orange_dark)
                    )
                    "approved" -> tvStatus.setTextColor(
                        ContextCompat.getColor(root.context, android.R.color.holo_green_dark)
                    )
                    "rejected" -> tvStatus.setTextColor(
                        ContextCompat.getColor(root.context, android.R.color.holo_red_dark)
                    )
                    else -> tvStatus.setTextColor(
                        ContextCompat.getColor(root.context, android.R.color.darker_gray)
                    )
                }

                root.setOnClickListener { onItemClick(item) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHwmsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun update(newList: List<DisplayItem>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}
