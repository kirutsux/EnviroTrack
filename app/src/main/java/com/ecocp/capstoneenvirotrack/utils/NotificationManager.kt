package com.ecocp.capstoneenvirotrack.utils

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

object NotificationManager {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Core function to send a notification document to the "notifications" collection.
     * Extra fields added to help UI grouping & deep-linking.
     */
    fun sendNotificationToUser(
        receiverId: String,
        title: String,
        message: String,
        category: String = "general",   // submission | alert | expiry | system | approval etc.
        priority: String = "medium",    // low | medium | high
        module: String? = null,         // e.g. "CNC", "OPMS", "PTO"
        documentId: String? = null,     // id of related application
        actionLink: String? = null      // optional route or deep link path in-app
    ) {
        val now = Timestamp.now()
        val dayString = dateToDayString(now.toDate())

        val payload = hashMapOf(
            "receiverId" to receiverId,
            "title" to title,
            "message" to message,
            "category" to category,
            "priority" to priority,
            "module" to module,
            "documentId" to documentId,
            "actionLink" to actionLink,
            "timestamp" to now,
            // duplicate day string to speed up grouping in queries/ui
            "dayString" to dayString,
            "isRead" to false
        )

        db.collection("notifications").add(payload)
    }

    /**
     * Sends the notification to every EMB user (use for admin-alerts).
     * This performs one query to fetch EMB users and writes one notification per EMB user.
     */
    fun sendToAllEmb(
        title: String,
        message: String,
        category: String = "alert",
        priority: String = "high",
        module: String? = null,
        documentId: String? = null,
        actionLink: String? = null
    ) {
        db.collection("users")
            .whereEqualTo("userType", "emb")
            .get()
            .addOnSuccessListener { snap ->
                for (user in snap.documents) {
                    val embId = user.id
                    sendNotificationToUser(
                        receiverId = embId,
                        title = title,
                        message = message,
                        category = category,
                        priority = priority,
                        module = module,
                        documentId = documentId,
                        actionLink = actionLink
                    )
                }
            }
    }

    /**
     * Mark a notification read/unread
     */
    fun markAsRead(notificationId: String, isRead: Boolean = true) {
        db.collection("notifications")
            .document(notificationId)
            .update(mapOf("isRead" to isRead))
    }

    fun deleteNotification(notificationId: String) {
        db.collection("notifications").document(notificationId).delete()
    }

    private fun dateToDayString(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(date)
    }
}
