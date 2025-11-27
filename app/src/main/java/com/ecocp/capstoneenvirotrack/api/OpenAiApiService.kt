package com.ecocp.capstoneenvirotrack.api

import com.ecocp.capstoneenvirotrack.model.ChatRequest
import com.ecocp.capstoneenvirotrack.model.ChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAiApiService {
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(@Body request: ChatRequest): ChatResponse
}