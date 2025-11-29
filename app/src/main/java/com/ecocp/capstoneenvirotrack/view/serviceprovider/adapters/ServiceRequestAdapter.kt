package com.ecocp.capstoneenvirotrack.view.serviceprovider.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.ItemServiceRequestBinding
import com.ecocp.capstoneenvirotrack.model.ServiceRequest

class ServiceRequestAdapter(
    private val requests: MutableList<ServiceRequest>,
    private val isActiveTasks: Boolean,
    private val onActionClick: (ServiceRequest) -> Unit,
    private var role: String = "transporter" // NEW: role support, default transporter
) : RecyclerView.Adapter<ServiceRequestAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemServiceRequestBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServiceRequestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = requests.size

    // ⭐ Called by Sorting / Filtering
    fun updateList(newList: List<ServiceRequest>) {
        requests.clear()
        requests.addAll(newList)
        notifyDataSetChanged()
    }

    // NEW: allow fragment to change role at runtime (call after role detection)
    fun setRole(role: String) {
        this.role = role.lowercase()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        holder.binding.apply {

            txtServiceTitle.text = request.serviceTitle
            txtCompanyName.text = request.companyName

            // ⭐ FIXED bookingStatus null handling
            val statusRaw = request.bookingStatus ?: "Pending"
            val status = statusRaw.lowercase()

            // Display
            bookingStatus.text = status.replaceFirstChar { it.uppercase() }
            bookingStatus.setBackgroundResource(R.drawable.bg_status_badge)

            // ⭐ Dynamic badge color
            val colorRes = when (status) {
                "confirmed", "completed" -> R.color.status_approved
                "rejected" -> R.color.status_rejected
                "pending", "paid" -> R.color.status_pending
                else -> R.color.status_pending
            }

            val badgeColor = ContextCompat.getColor(root.context, colorRes)
            bookingStatus.backgroundTintList = ColorStateList.valueOf(badgeColor)

            // ⭐ Button behavior adapts by role + activeTasks flag
            if (role == "tsd" || role == "tsdfacility" || request.serviceTitle.startsWith("TSD", true)) {
                // TSD view: prefer "Manage" for active tasks, otherwise "View"
                btnView.text = if (isActiveTasks) "Manage" else "View"
            } else {
                // Transporter / default behavior
                btnView.text = if (isActiveTasks) "Update Status" else "View"
            }

            // ⭐ Image (fallback avatar)
            Glide.with(imgClient.context)
                .load(
                    request.imageUrl.ifEmpty {
                        "https://i.pravatar.cc/150?img=3"
                    }
                )
                .circleCrop()
                .into(imgClient)

            // ⭐ Click callback
            btnView.setOnClickListener { onActionClick(request) }
        }
    }
}
