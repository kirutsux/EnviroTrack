package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.databinding.ItemChecklistBinding
import com.ecocp.capstoneenvirotrack.model.ChecklistItem

class ChecklistAdapter(
    private val onStatusChange: (String, String) -> Unit
) : ListAdapter<ChecklistItem, ChecklistAdapter.ViewHolder>(ChecklistDiffCallback()) {

    inner class ViewHolder(private val binding: ItemChecklistBinding) : RecyclerView.ViewHolder(binding.root) {

        private var currentItem: ChecklistItem? = null

        fun bind(item: ChecklistItem) {
            currentItem = item

            binding.tvItemName.text = item.name
            binding.tvItemDescription.text = item.description

            // Set spinner selection
            val position = when (item.status) {
                "Pending" -> 0
                "Submitted" -> 1
                "Approved" -> 2
                "Rejected" -> 3
                else -> 0
            }
            binding.spinnerStatus.setSelection(position, false) // false prevents triggering listener on bind

            // Set listener
            binding.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                    val newStatus = when (pos) {
                        0 -> "Pending"
                        1 -> "Submitted"
                        2 -> "Approved"
                        3 -> "Rejected"
                        else -> "Pending"
                    }

                    // Only trigger if status changed
                    if (currentItem?.status != newStatus) {
                        currentItem?.let { onStatusChange(it.id, newStatus) }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Optional: do nothing
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChecklistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ChecklistDiffCallback : DiffUtil.ItemCallback<ChecklistItem>() {
    override fun areItemsTheSame(oldItem: ChecklistItem, newItem: ChecklistItem) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: ChecklistItem, newItem: ChecklistItem) = oldItem == newItem
}
