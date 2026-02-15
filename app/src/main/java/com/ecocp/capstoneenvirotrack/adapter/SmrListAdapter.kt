package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.Smr

class SmrListAdapter(
    private val onItemClick: ((Smr) -> Unit)? = null
) : ListAdapter<Smr, SmrListAdapter.SmrViewHolder>(SmrDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmrViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_smr_list, parent, false)
        return SmrViewHolder(view)
    }

    override fun onBindViewHolder(holder: SmrViewHolder, position: Int) {
        val smr = getItem(position)
        holder.bind(smr)
    }

    inner class SmrViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSmrTitle: TextView = itemView.findViewById(R.id.tvSmrTitle)
        private val tvSmrDate: TextView = itemView.findViewById(R.id.tvSmrDate)

        fun bind(smr: Smr) {
            tvSmrTitle.text = "SMR Submission"

            // Format submittedAt timestamp to readable date
            val dateText = smr.dateSubmitted?.let { timestamp ->
                val sdf =
                    java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                "Submitted on ${sdf.format(timestamp.toDate())}"
            } ?: "Not submitted"

            tvSmrDate.text = dateText

            itemView.setOnClickListener {
                onItemClick?.invoke(smr)
            }
        }
    }

    class SmrDiffCallback : DiffUtil.ItemCallback<Smr>() {
        override fun areItemsTheSame(oldItem: Smr, newItem: Smr): Boolean {
            return oldItem.dateSubmitted == newItem.dateSubmitted
        }

        override fun areContentsTheSame(oldItem: Smr, newItem: Smr): Boolean {
            return oldItem == newItem
        }
    }
}
