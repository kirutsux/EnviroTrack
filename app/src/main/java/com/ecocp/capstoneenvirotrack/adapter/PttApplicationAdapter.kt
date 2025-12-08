package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.EmbPttApplication

class PttApplicationAdapter(
    private var applications: MutableList<EmbPttApplication>,
    private val onItemClick: ((EmbPttApplication) -> Unit)? = null
) : RecyclerView.Adapter<PttApplicationAdapter.PttAppViewHolder>() {

    inner class PttAppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGeneratorName: TextView = itemView.findViewById(R.id.tvGeneratorName)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvPaymentStatus: TextView = itemView.findViewById(R.id.tvPaymentStatus)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvSubmittedAt: TextView = itemView.findViewById(R.id.tvSubmittedAt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PttAppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ptt_application, parent, false)
        return PttAppViewHolder(view)
    }

    override fun onBindViewHolder(holder: PttAppViewHolder, position: Int) {
        val application = applications[position]

        holder.tvGeneratorName.text = application.generatorName ?: "Unknown"
        holder.tvAmount.text = "Amount: ${application.amount}"
        holder.tvPaymentStatus.text = "Payment: ${application.paymentStatus}"
        holder.tvStatus.text = "Status: ${application.status}"
        holder.tvSubmittedAt.text = application.submittedAtFormatted()

        // Click listener
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(application)
        }
    }

    override fun getItemCount(): Int = applications.size

    fun updateList(newList: List<EmbPttApplication>) {
        applications = newList.toMutableList()
        notifyDataSetChanged()
    }
}
