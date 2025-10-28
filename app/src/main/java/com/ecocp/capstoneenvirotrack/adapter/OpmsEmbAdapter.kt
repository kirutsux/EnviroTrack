package com.ecocp.capstoneenvirotrack.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.EmbOpmsApplication
import java.util.Locale

class OpmsEmbAdapter(
    private val applications: List<EmbOpmsApplication>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_PTO = 1
        private const val TYPE_DP = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (applications[position].applicationType) {
            "Permit to Operate" -> TYPE_PTO
            "Discharge Permit" -> TYPE_DP
            else -> TYPE_DP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_PTO) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pto_application, parent, false)
            PTOViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_dp_application, parent, false)
            DPViewHolder(view)
        }
    }

    override fun getItemCount(): Int = applications.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val app = applications[position]
        val bundle = Bundle().apply {
            putString("applicationId", app.applicationId)
        }

        val context: Context = holder.itemView.context
        val statusText = app.status ?: "Pending"
        val normalizedStatus = statusText.lowercase(Locale.getDefault())

        val colorRes = when (normalizedStatus) {
            "approved" -> R.color.status_approved
            "rejected" -> R.color.status_rejected
            "pending" -> R.color.status_pending
            else -> R.color.status_pending
        }
        val color = ContextCompat.getColor(context, colorRes)

        // ✅ Format dates
        val issueDateFormatted = app.issueDate?.toDate()?.let {
            android.text.format.DateFormat.format("MMM dd, yyyy", it)
        } ?: "-"
        val expiryDateFormatted = app.expiryDate?.toDate()?.let {
            android.text.format.DateFormat.format("MMM dd, yyyy", it)
        } ?: "-"

        val submittedDateFormatted = app.submittedTimestamp?.toDate()?.let {
            android.text.format.DateFormat.format("MMM dd, yyyy", it)
        } ?: "-"

        // ===== PTO HOLDER =====
        if (holder is PTOViewHolder) {
            holder.tvEstablishmentName.text = app.establishmentName ?: "-"
            holder.tvOwnerName.text = app.ownerName ?: "-"
            holder.tvPlantAddress.text = app.plantAddress ?: "-"
            holder.tvStatus.text = statusText.replaceFirstChar { it.uppercase() }
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_badge)
            holder.tvStatus.backgroundTintList = ColorStateList.valueOf(color)
            holder.tvApplicationType.text = app.applicationType ?: "Permit to Operate"
            holder.tvDateSubmitted.text = "Submitted: $submittedDateFormatted"

            // ✅ Show or hide issue/expiry date depending on status
            if (normalizedStatus == "approved") {
                holder.tvIssueDate.visibility = View.VISIBLE
                holder.tvExpiryDate.visibility = View.VISIBLE
                holder.tvIssueDate.text = "Issued: $issueDateFormatted"
                holder.tvExpiryDate.text = "Expires: $expiryDateFormatted"
            } else {
                holder.tvIssueDate.visibility = View.GONE
                holder.tvExpiryDate.visibility = View.GONE
            }

            holder.itemView.setOnClickListener {
                it.findNavController().navigate(
                    R.id.action_embDashboard_to_ptoDetailsFragment,
                    bundle
                )
            }

            // ===== DP HOLDER =====
        } else if (holder is DPViewHolder) {
            holder.tvCompanyName.text = app.companyName ?: "-"
            holder.tvCompanyAddress.text = app.companyAddress ?: "-"
            holder.tvReceivingBody.text = app.receivingBody ?: "-"
            holder.tvDischargeMethod.text = app.dischargeMethod ?: "-"
            holder.tvStatus.text = statusText.replaceFirstChar { it.uppercase() }
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_badge)
            holder.tvStatus.backgroundTintList = ColorStateList.valueOf(color)
            holder.tvApplicationType.text = app.applicationType ?: "Discharge Permit"
            holder.tvDateSubmitted.text = "Submitted: $submittedDateFormatted"

            // ✅ Show or hide issue/expiry date depending on status
            if (normalizedStatus == "approved") {
                holder.tvIssueDate.visibility = View.VISIBLE
                holder.tvExpiryDate.visibility = View.VISIBLE
                holder.tvIssueDate.text = "Issued: $issueDateFormatted"
                holder.tvExpiryDate.text = "Expires: $expiryDateFormatted"
            } else {
                holder.tvIssueDate.visibility = View.GONE
                holder.tvExpiryDate.visibility = View.GONE
            }

            holder.itemView.setOnClickListener {
                it.findNavController().navigate(
                    R.id.action_embDashboard_to_dpDetailsFragment,
                    bundle
                )
            }
        }
    }

    inner class PTOViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEstablishmentName: TextView = view.findViewById(R.id.tvEstablishmentName)
        val tvOwnerName: TextView = view.findViewById(R.id.tvOwnerName)
        val tvPlantAddress: TextView = view.findViewById(R.id.tvPlantAddress)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvApplicationType: TextView = view.findViewById(R.id.PermitType)
        val tvIssueDate: TextView = view.findViewById(R.id.tvIssueDate)
        val tvExpiryDate: TextView = view.findViewById(R.id.tvExpiryDate)
        val tvDateSubmitted: TextView = view.findViewById(R.id.tvDateSubmitted)
    }

    inner class DPViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCompanyName: TextView = view.findViewById(R.id.tvCompanyName)
        val tvCompanyAddress: TextView = view.findViewById(R.id.tvCompanyAddress)
        val tvReceivingBody: TextView = view.findViewById(R.id.tvReceivingBody)
        val tvDischargeMethod: TextView = view.findViewById(R.id.tvDischargeMethod)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvApplicationType: TextView = view.findViewById(R.id.PermitType)
        val tvIssueDate: TextView = view.findViewById(R.id.tvIssueDate)
        val tvExpiryDate: TextView = view.findViewById(R.id.tvExpiryDate)
        val tvDateSubmitted: TextView = view.findViewById(R.id.tvDateSubmitted)
    }
}