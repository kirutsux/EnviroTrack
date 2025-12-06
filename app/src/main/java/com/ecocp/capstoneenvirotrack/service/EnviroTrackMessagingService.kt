package com.ecocp.capstoneenvirotrack.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.view.all.COMP_Dashboard
import com.ecocp.capstoneenvirotrack.view.emb.all.EMB_Dashboard
import com.ecocp.capstoneenvirotrack.view.serviceprovider.SPMainActivity

class EnviroTrackMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            showNotification(it.title, it.body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToFirestore(token)
    }

    private fun saveTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).update("fcmToken", token)
    }

    private fun showNotification(title: String?, body: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "envirotrack_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "EnviroTrack Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Get userType from SharedPreferences
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userType = prefs.getString("userType", "service_provider") // default fallback

        val intent = when (userType) {
            "emb" -> Intent(this, EMB_Dashboard::class.java)
            "pco" -> Intent(this, COMP_Dashboard::class.java)
            "service_provider" -> Intent(this, SPMainActivity::class.java)
            else -> Intent(this, SPMainActivity::class.java)
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notifications)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }


    private fun showFinalNotification(
        notificationManager: NotificationManager,
        channelId: String,
        title: String?,
        body: String?,
        intent: Intent
    ) {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notifications)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
