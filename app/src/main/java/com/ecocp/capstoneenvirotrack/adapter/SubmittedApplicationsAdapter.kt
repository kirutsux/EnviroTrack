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
    private val onItemClick: (SubmittedApplication) -> Unit,
    private val onItemLongClick: (SubmittedApplication) -> Unit
) : RecyclerView.Adapter<SubmittedApplicationsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtPermitType: TextView = view.findViewById(R.id.txtPermitType)
        val txtPermitStatus: TextView = view.findViewById(R.id.txtPermitStatus)
        val txtSubmittedAt: TextView = view.findViewById(R.id.txtSubmittedAt)
        val txtIssueDate: TextView = view.findViewById(R.id.tvIssueDate)
        val txtExpiryDate: TextView = view.findViewById(R.id.tvExpiryDate)

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(applications[position])
                }
            }

            view.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick(applications[position])
                }
                true
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
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        // ✅ Permit Type
        val permitType = app.applicationType.ifEmpty { "Unknown Type" }
        holder.txtPermitType.text = permitType

        // ✅ Handle visibility and text for issue/expiry/submission date
        if (app.status.equals("approved", ignoreCase = true)) {
            holder.txtIssueDate.visibility = View.VISIBLE
            holder.txtExpiryDate.visibility = View.VISIBLE
            holder.txtSubmittedAt.visibility = View.GONE // hide "Submitted" line if showing issue/expiry

            val issuedDate = (app.issueDate as? Timestamp)?.toDate()
            val expiryDate = (app.expiryDate as? Timestamp)?.toDate()

            holder.txtIssueDate.text = if (issuedDate != null)
                "Issued: ${dateFormat.format(issuedDate)}" else "Issued: N/A"
            holder.txtExpiryDate.text = if (expiryDate != null)
                "Expires: ${dateFormat.format(expiryDate)}" else "Expires: N/A"
        } else {
            holder.txtIssueDate.visibility = View.GONE
            holder.txtExpiryDate.visibility = View.GONE
            holder.txtSubmittedAt.visibility = View.VISIBLE

            val formattedDate = when (val ts = app.timestamp) {
                is Timestamp -> dateFormat.format(ts.toDate())
                else -> app.timestamp?.toString() ?: "Unknown Date"
            }
            holder.txtSubmittedAt.text = "Submitted: $formattedDate"
        }

        // ✅ Dynamic Status Badge
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
