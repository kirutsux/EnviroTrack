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
import com.ecocp.capstoneenvirotrack.model.PcoAccreditation
import java.text.SimpleDateFormat
import java.util.*

class PcoEmbAdapter(
    private val pcoList: List<PcoAccreditation>,
    private val onItemClick: (PcoAccreditation) -> Unit
) : RecyclerView.Adapter<PcoEmbAdapter.PcoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PcoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pco_emb_card, parent, false)
        return PcoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PcoViewHolder, position: Int) {
        holder.bind(pcoList[position])
    }

    override fun getItemCount(): Int = pcoList.size

    inner class PcoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: CardView = itemView.findViewById(R.id.cardCnc)
        private val tvFullName: TextView = itemView.findViewById(R.id.tvFullName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvCompanyAffiliation: TextView = itemView.findViewById(R.id.tvCompanyAffiliation)
        private val tvPosition: TextView = itemView.findViewById(R.id.tvPosition)
        private val tvDateSubmitted: TextView = itemView.findViewById(R.id.tvDateSubmitted)

        @SuppressLint("SetTextI18n")
        fun bind(pco: PcoAccreditation) {
            tvFullName.text = pco.fullName ?: "N/A"
            tvCompanyAffiliation.text = pco.companyAffiliation ?: "N/A"
            tvPosition.text = "Position: ${pco.positionDesignation ?: "N/A"}"

            // Format timestamp
            val formattedDate = pco.submittedTimestamp?.toDate()?.let {
                SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(it)
            } ?: "Unknown Date"
            tvDateSubmitted.text = "Submitted: $formattedDate"

            // Set text and background badge
            tvStatus.text = pco.status ?: "Pending"

            val context = tvStatus.context
            val badgeDrawable = ContextCompat.getDrawable(context, R.drawable.bg_status_badge)
            val wrappedDrawable = DrawableCompat.wrap(badgeDrawable!!)

            val tintColor = when (pco.status?.lowercase()) {
                "approved" -> ContextCompat.getColor(context, R.color.status_approved)
                "rejected" -> ContextCompat.getColor(context, R.color.status_rejected)
                else -> ContextCompat.getColor(context, R.color.status_pending)
            }

            DrawableCompat.setTint(wrappedDrawable, tintColor)
            tvStatus.background = wrappedDrawable

            card.setOnClickListener { onItemClick(pco) }
        }
    }
}
