package com.ecocp.capstoneenvirotrack.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.Smr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmrEmbAdapter(
    private val smrList: List<Smr>,
    private val onItemClick: (Smr) -> Unit
) : RecyclerView.Adapter<SmrEmbAdapter.ViewHolder>() {

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val tvEstablishmentName: TextView = view.findViewById(R.id.tvEstablishmentName)
        val tvAddress: TextView = view.findViewById(R.id.tvAddress)
        val tvDateSubmitted: TextView = view.findViewById(R.id.tvDateSubmitted)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val card: CardView = view.findViewById(R.id.cardSmr)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_smr_emb_application_card, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val smr = smrList[position]
        holder.itemView.context
        val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

        holder.tvEstablishmentName.text = smr.generalInfo.establishmentName
        holder.tvAddress.text = smr.generalInfo.address
        holder.tvDateSubmitted.text = "Submitted: ${sdf.format(Date(smr.submittedAt ?: 0L))}"

        holder.tvStatus.text = smr.status
        val statusColor = when (smr.status) {
            "Reviewed" -> R.color.status_approved
            "Pending" -> R.color.status_pending
            else -> R.color.status_pending
        }
        holder.tvStatus.setBackgroundResource(statusColor)

        holder.card.setOnClickListener {onItemClick(smr)}
    }

    override fun getItemCount(): Int = smrList.size
}