package com.ecocp.capstoneenvirotrack.view.serviceprovider.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.databinding.ItemCompletedServiceBinding
import com.ecocp.capstoneenvirotrack.model.ServiceRequest

class CompletedServiceAdapter(
    private val completedList: MutableList<ServiceRequest>,
    private val onViewReportClick: (ServiceRequest) -> Unit
) : RecyclerView.Adapter<CompletedServiceAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCompletedServiceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCompletedServiceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = completedList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val service = completedList[position]
        holder.binding.apply {
            txtServiceTitle.text = service.serviceTitle
            txtCompanyName.text = service.companyName
            txtDateCompleted.text = service.dateRequested ?: "Completed"

            Glide.with(imgClient.context)
                .load(service.imageUrl.ifEmpty { "https://i.pravatar.cc/150?img=5" })
                .circleCrop()
                .into(imgClient)

            btnViewReport.setOnClickListener {
                onViewReportClick(service)
            }
        }
    }

    /** Replace adapter data cleanly */
    fun setData(newList: List<ServiceRequest>) {
        completedList.clear()
        completedList.addAll(newList)
        notifyDataSetChanged()
    }
}
