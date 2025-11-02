package com.ecocp.capstoneenvirotrack.view.businesses.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.PCO
import java.util.*

class PCOAdapter(
    private val list: MutableList<PCO>,
    private val onItemClick: (PCO) -> Unit
) : RecyclerView.Adapter<PCOAdapter.ViewHolder>(), Filterable {

    private var filteredList = list.toMutableList()

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

    override fun getItemCount(): Int = filteredList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = filteredList[position]
        val context = holder.itemView.context

        holder.tvAppId.text = app.appId
        holder.tvAppName.text = app.appName
        holder.tvApplicant.text = app.applicant
        holder.tvForwardedTo.text = app.forwardedTo
        holder.tvUpdated.text = app.updatedDate
        holder.tvType.text = app.type

        // ✅ Dynamic status badge color
        val status = app.status?.lowercase(Locale.getDefault()) ?: "pending"
        holder.tvStatus.text = status.replaceFirstChar { it.uppercase() }
        holder.tvStatus.setBackgroundResource(R.drawable.bg_status_badge)

        val colorRes = when (status) {
            "approved" -> R.color.status_approved
            "rejected" -> R.color.status_rejected
            "pending" -> R.color.status_pending
            "submitted" -> R.color.status_submitted
            else -> R.color.status_pending
        }

        val color = ContextCompat.getColor(context, colorRes)
        holder.tvStatus.backgroundTintList = ColorStateList.valueOf(color)

        holder.itemView.setOnClickListener {
            onItemClick(app)
        }
    }

    // ✅ Filtering logic retained
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString()?.lowercase(Locale.getDefault()) ?: ""
                val results = FilterResults()
                results.values = if (charString.isEmpty()) {
                    list
                } else {
                    list.filter {
                        it.appName.lowercase(Locale.getDefault()).contains(charString) ||
                                it.applicant.lowercase(Locale.getDefault()).contains(charString) ||
                                it.status.lowercase(Locale.getDefault()).contains(charString)
                    }
                }
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = (results?.values as? List<PCO>)?.toMutableList() ?: mutableListOf()
                notifyDataSetChanged()
            }
        }
    }

    fun filterByStatus(status: String) {
        filteredList = if (status == "All") {
            list.toMutableList()
        } else {
            list.filter { it.status.equals(status, ignoreCase = true) }.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun updateList(newList: List<PCO>) {
        list.clear()
        list.addAll(newList)
        filteredList = list.toMutableList()
        notifyDataSetChanged()
    }
}
