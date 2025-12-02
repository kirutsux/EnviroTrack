package com.ecocp.capstoneenvirotrack.utils

import android.util.Log
import com.ecocp.capstoneenvirotrack.api.RetrofitClient
import com.ecocp.capstoneenvirotrack.api.SendNotificationRequest
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object NotificationManager {
    private val db = FirebaseFirestore.getInstance()

    fun sendNotificationToUser(
        receiverId: String,
        title: String,
        message: String,
        category: String = "general",
        priority: String = "medium",
        module: String? = null,
        documentId: String? = null
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
            "timestamp" to now,
            "dayString" to dayString,
            "isRead" to false
        )

        // Save Firestore document
        db.collection("notifications").add(payload)

        // Send ONE push notification
        val request = SendNotificationRequest(receiverId, title, message)
        RetrofitClient.instance.sendNotification(request)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    Log.d("NotificationManager", "Push notification sent successfully")
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("NotificationManager", "Push notification error", t)
                }
            })
    }


    // FIXED: remove duplicate API calls
    fun sendToAllEmb(
        title: String,
        message: String,
        category: String = "alert",
        priority: String = "high",
        module: String? = null,
        documentId: String? = null,
        excludeUid: String? = null // <-- PCO UID to exclude
    ) {
        db.collection("users")
            .whereEqualTo("userType", "emb")
            .get()
            .addOnSuccessListener { snap ->
                for (user in snap.documents) {
                    val embId = user.id
                    // Skip the excluded UID (usually the submitting PCO)
                    if (excludeUid != null && embId == excludeUid) continue

                    // Save to Firestore & send push
                    sendNotificationToUser(
                        receiverId = embId,
                        title = title,
                        message = message,
                        category = category,
                        priority = priority,
                        module = module,
                        documentId = documentId
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e("NotificationManager", "Failed to fetch EMB users", e)
            }
    }


    fun markAsRead(notificationId: String, isRead: Boolean = true) =
        db.collection("notifications").document(notificationId)
            .update("isRead", isRead)

    fun deleteNotification(notificationId: String) {
        db.collection("notifications").document(notificationId).delete()
    }

    private fun dateToDayString(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(date)
    }
}

