package com.ecocp.capstoneenvirotrack.model

data class ChatRequest(
    val model: String,
    val messages: List<ApiMessage>
)

data class ApiMessage(
    val role: String,
    val content: String
)