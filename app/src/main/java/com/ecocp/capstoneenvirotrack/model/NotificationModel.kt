package com.ecocp.capstoneenvirotrack.model

import com.google.firebase.Timestamp

data class NotificationModel(
    var id: String? = null,             // local ID
    var documentId: String? = null,     // Firestore field must match
    var title: String = "",
    var message: String = "",
    var receiverId: String = "",
    var receiverType: String = "",
    var timestamp: Timestamp? = null,
    var type: String = "",
    var isRead: Boolean = false,
    var isHeader: Boolean = false,
    var dayString: String? = null,
    var module: String? = null,
    var priority: String? = null,
    var category: String? = null,
    var actionLink: String? = null
)
