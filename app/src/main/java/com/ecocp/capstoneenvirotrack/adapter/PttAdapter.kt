package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.databinding.ItemHwmsPttBinding
import com.ecocp.capstoneenvirotrack.model.DisplayItem

class PttAdapter(
    private var items: MutableList<DisplayItem>,
    private val onClick: (DisplayItem) -> Unit
) : RecyclerView.Adapter<PttAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemHwmsPttBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DisplayItem) {
            binding.apply {
                tvTitle.text = item.title
                tvTransport.text = "Transport Booking: ${item.transporter}"
                tvTsd.text = "TSD Booking: ${item.tsdFacility}"
                tvStatus.text = "Status: ${item.status}"

                root.setOnClickListener { onClick(item) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemHwmsPttBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(b)
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
