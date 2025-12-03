package com.ecocp.capstoneenvirotrack.model

data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val max_tokens: Int? =1000
)

data class OpenAiMessage(
    val role: String,
    val content: String
)