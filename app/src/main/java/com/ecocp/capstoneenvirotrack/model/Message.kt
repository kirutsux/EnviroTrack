package com.ecocp.capstoneenvirotrack.model

data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: String = "",
    val role: String,
    val content: String
)

