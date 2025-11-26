package com.ecocp.capstoneenvirotrack.model

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val index: Int,
    val message: ResponseMessage,
    val finishReason: String?
)

data class ResponseMessage(
    val role: String,
    val content: String
)
