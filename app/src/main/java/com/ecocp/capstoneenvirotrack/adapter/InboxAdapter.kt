package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.Provider

class InboxAdapter(
    private val providerList: List<Provider>,
    private val onItemClick: (Provider) -> Unit
) : RecyclerView.Adapter<InboxAdapter.InboxViewHolder>() {

    inner class InboxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        private val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        private val lastMessageText: TextView = itemView.findViewById(R.id.lastMessageText)

        fun bind(provider: Provider) {
            usernameText.text = provider.name
            lastMessageText.text = provider.description

            Glide.with(itemView.context)
                .load(provider.imageUrl)
                .placeholder(R.drawable.sample_profile)
                .into(profileImage)

            itemView.setOnClickListener { onItemClick(provider) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InboxViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inbox, parent, false)
        return InboxViewHolder(view)
    }

    override fun onBindViewHolder(holder: InboxViewHolder, position: Int) {
        holder.bind(providerList[position])
    }

    override fun getItemCount(): Int = providerList.size
}
