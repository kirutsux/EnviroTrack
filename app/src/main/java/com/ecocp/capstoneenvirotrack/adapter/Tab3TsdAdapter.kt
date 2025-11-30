package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.databinding.ItemTab3TsdBinding
import com.ecocp.capstoneenvirotrack.model.DisplayItem

class Tab3TsdAdapter(
    private var items: MutableList<DisplayItem>,
    private val onClick: (DisplayItem) -> Unit
) : RecyclerView.Adapter<Tab3TsdAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemTab3TsdBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DisplayItem) = with(binding) {
            tvFacility.text = item.tsdFacility
            tvSubtitle.text = item.subtitle
            tvTransporter.text = item.transporter
            tvStatus.text = item.status

            root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemTab3TsdBinding.inflate(
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
