package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.Crs

class CrsAdapter(
    private val list: MutableList<Crs>,
    private val onEditClick: (Crs) -> Unit,
    private val onDeleteClick: (Crs) -> Unit
) : RecyclerView.Adapter<CrsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCompanyId: TextView = view.findViewById(R.id.tvCompanyId)
        val tvEstablishment: TextView = view.findViewById(R.id.tvEstablishment)
        val tvCompanyType: TextView = view.findViewById(R.id.tvCompanyType)
        val tvCeoName: TextView = view.findViewById(R.id.tvCeoName)
        val tvAddress: TextView = view.findViewById(R.id.tvAddress)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvDateSubmitted: TextView = view.findViewById(R.id.tvDateSubmitted)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_establishment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = list[position]

        val shortRef = if (app.docId.length >= 8) app.docId.substring(0, 8) else app.docId
        holder.tvCompanyId.text = "Ref: $shortRef"
        holder.tvEstablishment.text = app.companyName
        holder.tvCompanyType.text = "Type: ${app.companyType}"
        holder.tvCeoName.text = "CEO: ${app.ceoName}"
        holder.tvAddress.text = app.address
        holder.tvStatus.text = "Status: ${app.status}"
        holder.tvDateSubmitted.text = app.dateSubmitted

        holder.btnEdit.setOnClickListener { onEditClick(app) }
        holder.btnDelete.setOnClickListener { onDeleteClick(app) }
    }

    override fun getItemCount(): Int = list.size
}