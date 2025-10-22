package com.ecocp.capstoneenvirotrack.model

data class Conversation(
    val receiverId: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0
)
