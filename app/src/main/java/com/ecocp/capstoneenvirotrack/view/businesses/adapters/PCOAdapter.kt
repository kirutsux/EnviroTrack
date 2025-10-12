package com.ecocp.capstoneenvirotrack.view.businesses.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
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
        val item = filteredList[position]

        holder.tvAppId.text = item.appId
        holder.tvAppName.text = item.appName
        holder.tvApplicant.text = item.applicant
        holder.tvForwardedTo.text = item.forwardedTo
        holder.tvUpdated.text = item.updatedDate
        holder.tvType.text = item.type
        holder.tvStatus.text = item.status

        applyStatusStyle(holder.tvStatus, item.status)

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    private fun applyStatusStyle(tv: TextView, status: String) {
        when (status.lowercase()) {
            "approved" -> {
                tv.setTextColor(tv.context.getColor(R.color.white))
                tv.setBackgroundResource(R.drawable.bg_badge_green)
            }
            "rejected" -> {
                tv.setTextColor(tv.context.getColor(R.color.white))
                tv.setBackgroundResource(R.drawable.bg_badge_red)
            }
            "submitted" -> {
                tv.setTextColor(tv.context.getColor(R.color.white))
                tv.setBackgroundResource(R.drawable.bg_badge_orange)
            }
            "pending" -> {
                tv.setTextColor(tv.context.getColor(R.color.white))
                tv.setBackgroundResource(R.drawable.bg_badge_blue)
            }
            else -> {
                tv.setTextColor(tv.context.getColor(R.color.black))
                tv.setBackgroundResource(R.drawable.bg_badge_gray)
            }
        }
    }

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
