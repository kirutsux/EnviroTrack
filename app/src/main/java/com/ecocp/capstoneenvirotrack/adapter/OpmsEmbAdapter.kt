package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.EmbOpmsApplication

class OpmsEmbAdapter(
    private val applications: List<EmbOpmsApplication>,
    private val onItemClick: (EmbOpmsApplication) -> Unit
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
        if (holder is PTOViewHolder) {
            holder.tvEstablishmentName.text = app.companyName ?: "-"
            holder.tvOwnerName.text = app.ownerName ?: "-"
            holder.tvPlantAddress.text = app.plantAddress ?: "-"
            holder.tvStatus.text = app.status ?: "Pending"
            holder.tvApplicationType.text = app.applicationType ?: "Permit to Operate"

            holder.itemView.setOnClickListener { onItemClick(app) }
        } else if (holder is DPViewHolder) {
            holder.tvCompanyName.text = app.companyName ?: "-"
            holder.tvCompanyAddress.text = app.companyAddress ?: "-"
            holder.tvReceivingBody.text = app.receivingBody ?: "-"
            holder.tvDischargeMethod.text = app.dischargeMethod ?: "-"
            holder.tvStatus.text = app.status ?: "Pending"
            holder.tvApplicationType.text = app.applicationType ?: "Discharge Permit"

            holder.itemView.setOnClickListener { onItemClick(app) }
        }
    }

    inner class PTOViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEstablishmentName: TextView = view.findViewById(R.id.tvEstablishmentName)
        val tvOwnerName: TextView = view.findViewById(R.id.tvOwnerName)
        val tvPlantAddress: TextView = view.findViewById(R.id.tvPlantAddress)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvApplicationType: TextView = view.findViewById(R.id.PermitType)
    }

    inner class DPViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCompanyName: TextView = view.findViewById(R.id.tvCompanyName)
        val tvCompanyAddress: TextView = view.findViewById(R.id.tvCompanyAddress)
        val tvReceivingBody: TextView = view.findViewById(R.id.tvReceivingBody)
        val tvDischargeMethod: TextView = view.findViewById(R.id.tvDischargeMethod)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvApplicationType: TextView = view.findViewById(R.id.PermitType)
    }
}
