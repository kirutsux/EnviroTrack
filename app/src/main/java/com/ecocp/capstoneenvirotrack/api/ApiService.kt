package com.ecocp.capstoneenvirotrack.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Call

data class EmailRequest(
    val to: String,
    val subject: String,
    val text: String
)

data class EmailResponse(
    val message: String,
    val id: String
)

interface ApiService {
    @POST("/send-email")
    fun sendEmail(@Body request: EmailRequest): Call<EmailResponse>
}
