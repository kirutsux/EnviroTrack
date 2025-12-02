package com.ecocp.capstoneenvirotrack.adapter

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.NavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.NotificationModel
import com.ecocp.capstoneenvirotrack.utils.NotificationManager

class NotificationAdapter(
    private val notificationList: MutableList<NotificationModel>,
    private val navController: NavController
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (notificationList[position].isHeader) TYPE_HEADER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            NotificationViewHolder(view)
        }
    }

    override fun getItemCount(): Int = notificationList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val notif = notificationList[position]

        if (holder is HeaderViewHolder) {
            holder.headerTitle.text = notif.title
        } else if (holder is NotificationViewHolder) {
            holder.title.text = notif.title
            holder.message.text = notif.message
            holder.time.text = notif.timestamp?.toDate()?.toLocaleString() ?: ""

            // Unread styling
            if (!notif.isRead) {
                holder.title.setTypeface(null, Typeface.BOLD)
                holder.leftBar.visibility = View.VISIBLE
                holder.unreadDot.visibility = View.VISIBLE
            } else {
                holder.title.setTypeface(null, Typeface.NORMAL)
                holder.leftBar.visibility = View.GONE
                holder.unreadDot.visibility = View.GONE
            }

            // Click listener
            holder.itemView.setOnClickListener {
                Log.d("NotificationAdapter", "Notification clicked: ${notif.title}, actionLink=${notif.actionLink}")

                if (!notif.isRead) {
                    notif.id?.let { id -> NotificationManager.markAsRead(id) }
                    notif.isRead = true
                    notifyItemChanged(position)
                }

                notif.actionLink?.let { link ->
                    val parts = link.split("/")
                    val bundle = Bundle().apply { if (parts.size >= 2) putString("documentId", parts[1]) }

                    when (parts[0]) {
                        "cncDashboard" -> navController.navigate(R.id.action_global_cncDashboardFragment, bundle)
                        "cncEmbDashboard" -> navController.navigate(R.id.action_global_embcncDashboardFragment, bundle)
                        "crsDashboard" -> navController.navigate(R.id.action_global_crsDashboardFragment, bundle)
                        "crsEmbDashboard" -> navController.navigate(R.id.action_global_embcrsDashboardFragment, bundle)
                        "ptoDashboard" -> navController.navigate(R.id.action_global_opmsDashboardFragment, bundle)
                        "ptoEmbDashboard" -> navController.navigate(R.id.action_global_embopmsDashboardFragment, bundle)
                        "dpDashboard" -> navController.navigate(R.id.action_global_opmsDashboardFragment, bundle)
                        "dpEmbDashboard" -> navController.navigate(R.id.action_global_embopmsDashboardFragment, bundle)
                        "accreditationDashboard" -> navController.navigate(R.id.action_global_pcoaccDashboardFragment, bundle)
                        "accreditationEmbDashboard" -> navController.navigate(R.id.action_global_embpcoaccDashboardFragment, bundle)
                    }

                    Log.d("NotificationAdapter", "Navigation executed for ${parts[0]} with documentId=${parts.getOrNull(1)}")
                }
            }

            // Long press: delete
            holder.itemView.setOnLongClickListener {
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Delete Notification")
                    .setMessage("Are you sure you want to delete this notification?")
                    .setPositiveButton("Yes") { _, _ ->
                        notif.id?.let { id ->
                            NotificationManager.deleteNotification(id)
                            notificationList.removeAt(position)
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
