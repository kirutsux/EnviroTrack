package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.ItemTab4PttBinding
import com.ecocp.capstoneenvirotrack.model.DisplayItem

class Tab4PttAdapter(
    private var items: MutableList<DisplayItem> = mutableListOf(),
    private val onClick: (DisplayItem) -> Unit = {}
) : RecyclerView.Adapter<Tab4PttAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemTab4PttBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DisplayItem) = with(binding) {
            tvTitle.text = item.title
            tvSubtitle.text = item.subtitle
            tvTransport.text = item.transporter
            tvTsd.text = item.tsdFacility
            tvPermitNo.text = "Permit: ${item.permitNo}"
            tvStatus.text = item.status

            // Use your existing beautiful colors
            val colorRes = when (item.status.lowercase()) {
                "approved"                  -> R.color.status_approved     // Green
                "rejected"                  -> R.color.status_rejected     // Red
                "submitted", "pending review", "pending" -> R.color.blue_pending  // Blue
                else                                        -> R.color.status_submitted // Orange fallback
            }

            tvStatus.setTextColor(ContextCompat.getColor(root.context, colorRes))

            root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTab4PttBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<DisplayItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}