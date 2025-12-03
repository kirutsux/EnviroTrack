package com.ecocp.capstoneenvirotrack.adapter

import android.app.AlertDialog
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.NotificationModel
import com.ecocp.capstoneenvirotrack.utils.NotificationManager

class NotificationAdapter(
    notifications: List<NotificationModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    // Maintain a unique list based on documentId
    private val notificationList: MutableList<NotificationModel> = mutableListOf()
    private val uniqueIds: MutableSet<String> = mutableSetOf()

    init {
        addNotifications(notifications)
    }

    fun addNotifications(notifications: List<NotificationModel>) {
        notifications.forEach { notif ->
            if (notif.documentId != null && !uniqueIds.contains(notif.documentId)) {
                notificationList.add(notif)
                uniqueIds.add(notif.documentId!!)
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (notificationList[position].isHeader) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_notification_header, parent, false))
        } else {
            NotificationViewHolder(inflater.inflate(R.layout.item_notification, parent, false))
        }
    }

    override fun getItemCount() = notificationList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val notif = notificationList[position]

        if (holder is HeaderViewHolder) {
            holder.headerTitle.text = notif.title
            return
        }

        if (holder is NotificationViewHolder) {
            holder.title.text = notif.title
            holder.message.text = notif.message
            holder.time.text = notif.timestamp?.toDate()?.toLocaleString() ?: ""

            // Read/unread UI
            if (!notif.isRead) {
                holder.title.setTypeface(null, Typeface.BOLD)
                holder.leftBar.visibility = View.VISIBLE
                holder.unreadDot.visibility = View.VISIBLE
            } else {
                holder.title.setTypeface(null, Typeface.NORMAL)
                holder.leftBar.visibility = View.GONE
                holder.unreadDot.visibility = View.GONE
            }

            // Click to mark as read
            holder.itemView.setOnClickListener {
                if (!notif.isRead && notif.documentId != null) {
                    NotificationManager.markAsRead(notif.documentId!!)
                    notif.isRead = true
                    notifyItemChanged(position)
                }
            }

            // Long press to delete
            holder.itemView.setOnLongClickListener {
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Delete Notification")
                    .setMessage("Are you sure you want to delete this notification?")
                    .setPositiveButton("Yes") { _, _ ->
                        notif.documentId?.let {
                            NotificationManager.deleteNotification(it)
                            notificationList.removeAt(position)
                            uniqueIds.remove(it)
                            notifyItemRemoved(position)
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()
                true
            }
        }
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerTitle: TextView = view.findViewById(R.id.headerTitle)
    }

    inner class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.textTitle)
        val message: TextView = view.findViewById(R.id.textMessage)
        val time: TextView = view.findViewById(R.id.textTime)
        val leftBar: View = view.findViewById(R.id.leftBar)
        val unreadDot: View = view.findViewById(R.id.unreadIndicator)
    }
}
