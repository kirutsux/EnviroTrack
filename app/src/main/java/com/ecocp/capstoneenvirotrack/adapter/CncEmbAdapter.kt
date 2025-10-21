package com.ecocp.capstoneenvirotrack.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.CncApplication
import java.text.SimpleDateFormat
import java.util.Locale

class CncEmbAdapter(
    private val cncList: List<CncApplication>,
    private val onItemClick: (CncApplication) -> Unit
) : RecyclerView.Adapter<CncEmbAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProjectTitle: TextView = view.findViewById(R.id.tvProjectTitle)
        val tvCompanyName: TextView = view.findViewById(R.id.tvCompanyName)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvDateSubmitted: TextView = view.findViewById(R.id.tvDateSubmitted)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val card: CardView = view.findViewById(R.id.cardCnc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emb_cnc_application, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cnc = cncList[position]
        val context: Context = holder.itemView.context
        val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

        holder.tvProjectTitle.text = cnc.projectTitle ?: "No Title"
        holder.tvCompanyName.text = cnc.companyName ?: "No Company"
        holder.tvLocation.text = "Location: ${cnc.projectLocation ?: "N/A"}"
        holder.tvDateSubmitted.text =
            "Submitted: ${sdf.format(cnc.submittedTimestamp?.toDate() ?: java.util.Date())}"

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

        holder.card.setOnClickListener { onItemClick(cnc) }
    }

    override fun getItemCount() = cncList.size
}
