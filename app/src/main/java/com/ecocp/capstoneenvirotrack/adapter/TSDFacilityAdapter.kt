package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.databinding.ItemTsdFacilityBinding
import com.ecocp.capstoneenvirotrack.model.TSDFacility

class TSDFacilityAdapter(
    private val facilities: List<TSDFacility>,
    private val onSelect: (TSDFacility) -> Unit
) : RecyclerView.Adapter<TSDFacilityAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemTsdFacilityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(facility: TSDFacility) {
            binding.tvFacilityName.text =
                "Company: ${facility.companyName.ifEmpty { "Unknown Facility" }}"
            binding.tvLocation.text =
                "Location: ${facility.location.ifEmpty { "Not specified" }}"
            binding.tvContact.text =
                "Contact: ${facility.contactNumber.ifEmpty { "N/A" }}"

            binding.root.setOnClickListener {
                onSelect(facility)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTsdFacilityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(facilities[position])
    }

    override fun getItemCount() = facilities.size
}
