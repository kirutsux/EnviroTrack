package com.ecocp.capstoneenvirotrack.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.InboxItem

class InboxAdapter(
    private val itemList: List<InboxItem>,
    private val onItemClick: (InboxItem) -> Unit
) : RecyclerView.Adapter<InboxAdapter.InboxViewHolder>() {

    inner class InboxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        private val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        private val lastMessageText: TextView = itemView.findViewById(R.id.lastMessageText)

        fun bind(item: InboxItem) {
            usernameText.text = item.name
            lastMessageText.text = item.description

            Glide.with(itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.sample_profile)
                .into(profileImage)

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InboxViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inbox, parent, false)
        return InboxViewHolder(view)
    }

    override fun onBindViewHolder(holder: InboxViewHolder, position: Int) {
        holder.bind(itemList[position])
    }

    override fun getItemCount(): Int = itemList.size
}
