package com.ecocp.capstoneenvirotrack.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.databinding.ItemHwmsWasteBinding
import com.ecocp.capstoneenvirotrack.model.WasteGenDisplay

class WasteGenAdapter(
    private var items: MutableList<WasteGenDisplay>,
    private val onClick: (WasteGenDisplay) -> Unit
) : RecyclerView.Adapter<WasteGenAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemHwmsWasteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WasteGenDisplay) {
            binding.apply {
                tvCompanyName.text = item.companyName
                tvEmbNo.text = "EMB Reg No: ${item.embRegNo}"
                tvStatus.text = "Status: ${item.status}"
                tvDate.text = "Submitted: ${item.timestamp}"

                // clear list first
                containerWasteList.removeAllViews()

                // dynamically add waste items
                item.wasteList.forEach { waste ->
                    val tv = TextView(root.context).apply {
                        text = "• ${waste.wasteName} — ${waste.quantity} (${waste.wasteCode})"
                        textSize = 14f
                        setTextColor(Color.parseColor("#444444"))
                    }
                    containerWasteList.addView(tv)
                }

                root.setOnClickListener { onClick(item) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHwmsWasteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<WasteGenDisplay>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
