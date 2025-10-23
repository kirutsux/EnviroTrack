package com.ecocp.capstoneenvirotrack.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.CrsApplication
import java.text.SimpleDateFormat
import java.util.*

class CrsEmbAdapter(
    private val crsList: List<CrsApplication>,
    private val onItemClick: (CrsApplication) -> Unit
) : RecyclerView.Adapter<CrsEmbAdapter.CrsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_crs_emb_application, parent, false)
        return CrsViewHolder(view)
    }

    override fun onBindViewHolder(holder: CrsViewHolder, position: Int) {
        holder.bind(crsList[position])
    }

    override fun getItemCount(): Int = crsList.size

    inner class CrsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: CardView = itemView.findViewById(R.id.cardCrs)
        private val tvCompanyName: TextView = itemView.findViewById(R.id.txtCompanyName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvNatureOfBusiness: TextView = itemView.findViewById(R.id.tvNatureOfBusiness)
        private val tvCompanyType: TextView = itemView.findViewById(R.id.txtType)
        private val tvDateSubmitted: TextView = itemView.findViewById(R.id.txtDateSubmitted)

        @SuppressLint("SetTextI18n")
        fun bind(crs: CrsApplication) {
            tvCompanyName.text = crs.companyName.ifBlank { "N/A" }
            tvNatureOfBusiness.text = "Nature: ${crs.natureOfBusiness.ifBlank { "N/A" }}"
            tvCompanyType.text = "Type: ${crs.companyType.ifBlank { "N/A" }}"

            // 🗓 Format date
            val formattedDate = crs.dateSubmitted?.toDate()?.let {
                SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(it)
            } ?: "Unknown Date"
            tvDateSubmitted.text = "Submitted: $formattedDate"

            // 🎨 Status badge color
            tvStatus.text = crs.status.ifBlank { "Pending" }
            val context = tvStatus.context
            val badgeDrawable = ContextCompat.getDrawable(context, R.drawable.bg_status_badge)
            val wrappedDrawable = DrawableCompat.wrap(badgeDrawable!!)

            val tintColor = when (crs.status.lowercase(Locale.getDefault())) {
                "approved" -> ContextCompat.getColor(context, R.color.status_approved)
                "rejected" -> ContextCompat.getColor(context, R.color.status_rejected)
                else -> ContextCompat.getColor(context, R.color.status_pending)
            }

            DrawableCompat.setTint(wrappedDrawable, tintColor)
            tvStatus.background = wrappedDrawable

            // 📦 Card click
            card.setOnClickListener { onItemClick(crs) }
        }
    }
}
