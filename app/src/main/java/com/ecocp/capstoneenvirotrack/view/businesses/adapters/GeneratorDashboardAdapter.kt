package com.ecocp.capstoneenvirotrack.view.businesses.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.databinding.ItemGeneratorApplicationBinding
import com.ecocp.capstoneenvirotrack.view.businesses.hwms.GeneratorApplication

class GeneratorDashboardAdapter(
    private val items: List<GeneratorApplication>
) : RecyclerView.Adapter<GeneratorDashboardAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemGeneratorApplicationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGeneratorApplicationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // ✅ Bind only necessary summary fields for the dashboard
        holder.binding.txtCompanyName.text = item.companyName
        holder.binding.txtEstablishmentName.text = "Establishment: ${item.establishmentName}"
        holder.binding.txtPcoName.text = "PCO: ${item.pcoName}"
        holder.binding.txtNatureOfBusiness.text = "Business: ${item.natureOfBusiness}"
        holder.binding.txtStatus.text = "Status: ${item.status}"
        holder.binding.txtDate.text = "Submitted on: ${item.dateSubmitted}"
    }

    override fun getItemCount() = items.size
}
