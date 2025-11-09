package com.ecocp.capstoneenvirotrack.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

        holder.tvStatus.text = "Status: ${cnc.status ?: "Pending"}"

        holder.itemView.setOnClickListener {
            onItemClick(cnc)
        }
    }

    override fun getItemCount(): Int = cncList.size
}
