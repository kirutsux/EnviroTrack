package com.ecocp.capstoneenvirotrack.view.serviceprovider.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.databinding.ItemServiceRequestBinding
import com.ecocp.capstoneenvirotrack.model.ServiceRequest

class ServiceRequestAdapter(
    private val requests: List<ServiceRequest>,
    private val onViewClick: (ServiceRequest) -> Unit
) : RecyclerView.Adapter<ServiceRequestAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemServiceRequestBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServiceRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = requests.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        holder.binding.apply {
            txtServiceTitle.text = request.serviceTitle
            txtCompanyName.text = request.companyName
            txtStatus.text = request.status
            txtCompliance.text = request.compliance

            // 🟩 Set button text based on request status
            btnView.text = if (request.status == "Completed") "View" else "Update Status"

            // Change background color based on status
            when (request.status) {
                "Pending" -> txtStatus.setBackgroundColor(Color.parseColor("#f39c12"))
                "In Progress" -> txtStatus.setBackgroundColor(Color.parseColor("#2980b9"))
                "Completed" -> txtStatus.setBackgroundColor(Color.parseColor("#27ae60"))
            }

            Glide.with(imgClient.context)
                .load(request.imageUrl.ifEmpty { "https://i.pravatar.cc/150?img=3" })
                .circleCrop()
                .into(imgClient)

            btnView.setOnClickListener { onViewClick(request) }
        }
    }
}