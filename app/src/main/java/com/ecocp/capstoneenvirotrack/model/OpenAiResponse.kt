package com.ecocp.capstoneenvirotrack.model

data class OpenAiResponse(
    val choices: List<OpenAiChoice>
)

data class OpenAiChoice(
    val message: Messages
)

data class Messages(
    val content: String
)