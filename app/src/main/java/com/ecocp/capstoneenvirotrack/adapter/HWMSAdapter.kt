package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.databinding.ItemHwmsBinding
import com.ecocp.capstoneenvirotrack.model.HWMSApplication

class HWMSAdapter(
    private var applications: MutableList<HWMSApplication>,
    private val onItemClick: (HWMSApplication) -> Unit
) : RecyclerView.Adapter<HWMSAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemHwmsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(application: HWMSApplication) {
            binding.apply {
                tvWasteType.text = application.wasteType
                tvQuantity.text = "Quantity: ${application.quantity} ${application.unit}"
                tvTransporter.text = "Transporter: ${application.transporterName}"
                tvTsdFacility.text = "TSD Facility: ${application.tsdFacilityName}"
                tvPermitNo.text = "Permit No: ${application.permitNumber ?: "Pending"}"
                tvPaymentStatuss.text = "Payment: ${application.paymentStatus ?: "Unpaid"}"

                // Display color-coded status text
                tvStatus.text = application.status
                when (application.status.lowercase()) {
                    "pending" -> tvStatus.setTextColor(root.context.getColor(android.R.color.holo_orange_dark))
                    "approved" -> tvStatus.setTextColor(root.context.getColor(android.R.color.holo_green_dark))
                    "rejected" -> tvStatus.setTextColor(root.context.getColor(android.R.color.holo_red_dark))
                    else -> tvStatus.setTextColor(root.context.getColor(android.R.color.darker_gray))
                }

                root.setOnClickListener { onItemClick(application) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHwmsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(applications[position])
    }

    override fun getItemCount(): Int = applications.size

    // Optional helper methods
    fun updateData(newList: List<HWMSApplication>) {
        applications.clear()
        applications.addAll(newList)
        notifyDataSetChanged()
    }

    fun addApplication(newApp: HWMSApplication) {
        applications.add(0, newApp)
        notifyItemInserted(0)
    }
}
