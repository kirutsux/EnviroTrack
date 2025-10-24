package com.ecocp.capstoneenvirotrack.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.SubmittedApplication
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class SubmittedApplicationsAdapter(
    private val applications: List<SubmittedApplication>,
    private val onItemClick: (SubmittedApplication) -> Unit
) : RecyclerView.Adapter<SubmittedApplicationsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtPermitType: TextView = view.findViewById(R.id.txtPermitType)
        val txtPermitStatus: TextView = view.findViewById(R.id.txtPermitStatus)
        val txtSubmittedAt: TextView = view.findViewById(R.id.txtSubmittedAt)

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(applications[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_submitted_application, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = applications[position]

        // ✅ Permit type
        val permitType = app.applicationType.ifEmpty { "Unknown Type" }
        holder.txtPermitType.text = permitType

        // ✅ Handle Timestamp safely
        val formattedDate = when (val ts = app.timestamp) {
            is Timestamp -> {
                val date = ts.toDate()
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
            }
            else -> app.timestamp?.toString() ?: "Unknown Date"
        }
        holder.txtSubmittedAt.text = "Submitted: $formattedDate"

        // ✅ Dynamic status badge
        val status = app.status?.lowercase(Locale.getDefault()) ?: "pending"
        holder.txtPermitStatus.text = status.replaceFirstChar { it.uppercase() }
        holder.txtPermitStatus.setBackgroundResource(R.drawable.bg_status_badge)

        val colorRes = when (status) {
            "approved" -> R.color.status_approved
            "rejected" -> R.color.status_rejected
            "pending" -> R.color.status_pending
            else -> R.color.status_pending
        }

        val color = ContextCompat.getColor(holder.itemView.context, colorRes)
        holder.txtPermitStatus.backgroundTintList = ColorStateList.valueOf(color)
    }

    override fun getItemCount(): Int = applications.size
}
