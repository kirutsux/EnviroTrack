package com.ecocp.capstoneenvirotrack.view.businesses.adapters
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.PCO

class PCOAdapter(
    private val list: List<PCO>
) : RecyclerView.Adapter<PCOAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAppId: TextView = itemView.findViewById(R.id.tvAppId)
        val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        val tvApplicant: TextView = itemView.findViewById(R.id.tvApplicant)
        val tvForwardedTo: TextView = itemView.findViewById(R.id.tvForwardedTo)
        val tvUpdated: TextView = itemView.findViewById(R.id.tvUpdated)
        val tvType: TextView = itemView.findViewById(R.id.tvType)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pco_application, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvAppId.text = item.appId
        holder.tvAppName.text = item.appName
        holder.tvApplicant.text = item.applicant
        holder.tvForwardedTo.text = item.forwardedTo
        holder.tvUpdated.text = item.updatedDate
        holder.tvType.text = item.type
        holder.tvStatus.text = item.status

        // 🔹 Optional: Status color logic
        when (item.status.lowercase()) {
            "approved" -> holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_green)
            "rejected" -> holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_red)
            "submitted" -> holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_orange)
            else -> holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_gray)
        }
    }
}