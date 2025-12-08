package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.model.Regulation
import com.ecocp.capstoneenvirotrack.databinding.ItemRegulationBinding
import java.text.SimpleDateFormat
import java.util.Locale

class RegulationAdapter(
    private val onItemClick: (Regulation) -> Unit,
    private val onDownloadClick: (Regulation) -> Unit
) : ListAdapter<Regulation, RegulationAdapter.RegulationViewHolder>(RegulationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegulationViewHolder {
        val binding = ItemRegulationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RegulationViewHolder(binding, onItemClick, onDownloadClick)
    }

    override fun onBindViewHolder(holder: RegulationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RegulationViewHolder(
        private val binding: ItemRegulationBinding,
        private val onItemClick: (Regulation) -> Unit,
        private val onDownloadClick: (Regulation) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(regulation: Regulation) {
            binding.apply {
                tvRegulationName.text = regulation.regulationName
                tvRepublicAct.text = "Republic Act: ${regulation.republicAct}"
                tvDescription.text = regulation.description

                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                tvCreatedDate.text = regulation.createdAt?.let {
                    "Created: ${dateFormat.format(it)}"
                } ?: "Created: N/A"

                root.setOnClickListener {
                    onItemClick(regulation)
                }

                btnDownload.setOnClickListener {
                    onDownloadClick(regulation)
                }
            }
        }
    }

    private class RegulationDiffCallback : DiffUtil.ItemCallback<Regulation>() {
        override fun areItemsTheSame(oldItem: Regulation, newItem: Regulation) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Regulation, newItem: Regulation) =
            oldItem == newItem
    }
}