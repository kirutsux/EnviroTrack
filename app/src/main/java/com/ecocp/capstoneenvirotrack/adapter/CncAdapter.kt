package com.ecocp.capstoneenvirotrack.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.CncApplication
import java.text.SimpleDateFormat
import java.util.Locale

class CncAdapter(
    private val cncList: List<CncApplication>,
    private val context: Context,
    private val onItemClick: (CncApplication) -> Unit
) : RecyclerView.Adapter<CncAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProjectTitle: TextView = itemView.findViewById(R.id.tvProjectTitle)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvDateSubmitted: TextView = itemView.findViewById(R.id.tvDateSubmitted)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_cnc_application, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cnc = cncList[position]

        holder.tvProjectTitle.text = cnc.projectTitle ?: "Untitled Project"
        holder.tvLocation.text = "Location: ${cnc.projectLocation ?: "N/A"}"

        val formattedDate = cnc.submittedTimestamp?.toDate()?.let {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
        } ?: "Not submitted"
        holder.tvDateSubmitted.text = "Submitted: $formattedDate"

        // ✅ Dynamic status badge color
        val status = cnc.status?.lowercase(Locale.getDefault()) ?: "pending"
        holder.tvStatus.text = status.replaceFirstChar { it.uppercase() }
        holder.tvStatus.setBackgroundResource(R.drawable.bg_status_badge)

        val colorRes = when (status) {
            "approved" -> R.color.status_approved
            "rejected" -> R.color.status_rejected
            "pending" -> R.color.status_pending
            else -> R.color.status_pending
        }

        val color = ContextCompat.getColor(context, colorRes)
        holder.tvStatus.backgroundTintList = ColorStateList.valueOf(color)

        holder.itemView.setOnClickListener {
            onItemClick(cnc)
        }
    }

    override fun getItemCount(): Int = cncList.size
}
