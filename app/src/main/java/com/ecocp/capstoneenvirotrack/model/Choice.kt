package com.ecocp.capstoneenvirotrack.model

data class Choice(
    val index: Int,
    val message: ResponseMessage,
    val finishReason: String?
)
