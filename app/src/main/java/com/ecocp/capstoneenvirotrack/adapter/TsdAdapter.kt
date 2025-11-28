package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.databinding.ItemHwmsTsdBinding
import com.ecocp.capstoneenvirotrack.model.DisplayItem

class TsdAdapter(
    private var items: MutableList<DisplayItem>,
    private val onClick: (DisplayItem) -> Unit
) : RecyclerView.Adapter<TsdAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemHwmsTsdBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DisplayItem) {
            binding.apply {
                tvFacility.text = item.tsdFacility
                tvSubtitle.text = item.subtitle
                tvTransporter.text = item.transporter
                tvStatus.text = item.status

                root.setOnClickListener { onClick(item) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemHwmsTsdBinding.inflate(
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
