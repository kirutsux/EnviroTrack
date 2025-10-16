package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.SubmittedApplication

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
        holder.txtPermitType.text = "Discharge Permit"
        holder.txtPermitStatus.text = "Status: ${app.status}"
        holder.txtSubmittedAt.text = "Submitted: ${app.timestamp}"
    }

    override fun getItemCount() = applications.size
}
