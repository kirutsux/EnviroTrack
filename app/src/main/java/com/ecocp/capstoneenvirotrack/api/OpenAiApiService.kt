package com.ecocp.capstoneenvirotrack.api

import com.ecocp.capstoneenvirotrack.model.OpenAiRequest
import com.ecocp.capstoneenvirotrack.model.OpenAiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAiApiService {
    @POST("v1/chat/completion")
    suspend fun getChatCompletion(@Body request: OpenAiRequest): OpenAiResponse
}