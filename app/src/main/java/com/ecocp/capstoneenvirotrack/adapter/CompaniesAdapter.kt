package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.CompanySummary

class CompaniesAdapter(
    private val items: List<CompanySummary>,
    private val onItemClick: (CompanySummary) -> Unit
) : RecyclerView.Adapter<CompaniesAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvCompanyName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvCompanyStatus)
        val tvExpiry: TextView = itemView.findViewById(R.id.tvNearestExpiry)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_company_card, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.tvName.text = c.companyName
        holder.tvStatus.text = "Status: ${c.overallStatus}"

        val nearestExpiry = listOfNotNull(c.latestPtoExpiry, c.latestDpExpiry, c.pcoExpiry).minOrNull()
        holder.tvExpiry.text = if (nearestExpiry != null) {
            val days = ((nearestExpiry.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24))
            "Nearest Expiry: $days day(s)"
        } else {
            "Nearest Expiry: --"
        }
        // ✅ Set chip-style background and white text
        val statusView = holder.tvStatus
        val context = holder.itemView.context

        if (c.overallStatus.equals("Compliant", true)) {
            statusView.setBackgroundResource(R.drawable.status_chip_bg)
        } else {
            statusView.setBackgroundResource(R.drawable.status_chip_red_bg)
        }

        // Always white text
        statusView.setTextColor(ContextCompat.getColor(context, R.color.white))

        holder.itemView.setOnClickListener { onItemClick(c) }
    }
}
