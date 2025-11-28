package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.databinding.ItemHwmsTransportBinding
import com.ecocp.capstoneenvirotrack.model.DisplayItem

class TransportAdapter(
    private var items: MutableList<DisplayItem>,
    private val onClick: (DisplayItem) -> Unit
) : RecyclerView.Adapter<TransportAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemHwmsTransportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DisplayItem) {
            binding.apply {
                tvTitle.text = item.title
                tvSubtitle.text = item.subtitle
                tvTransporter.text = item.transporter
                tvTsd.text = item.tsdFacility
                tvStatus.text = item.status

                root.setOnClickListener { onClick(item) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemHwmsTransportBinding.inflate(
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
