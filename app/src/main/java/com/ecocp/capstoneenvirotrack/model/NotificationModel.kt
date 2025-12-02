package com.ecocp.capstoneenvirotrack.model

import com.google.firebase.Timestamp

data class NotificationModel(
    var id: String? = null,
    var title: String = "",
    var message: String = "",
    var receiverId: String = "",
    var receiverType: String = "",
    var timestamp: Timestamp? = null,
    var type: String = "",
    var isRead: Boolean = false,
    var isHeader: Boolean = false, // 👈 add this
    val actionLink: String? = null  // <-- add this
)