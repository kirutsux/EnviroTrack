package com.ecocp.capstoneenvirotrack.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

// ----------------- Email -----------------
data class EmailRequest(
    val to: String,
    val subject: String,
    val text: String,
    val html: String? = null // optional HTML content
)

data class EmailResponse(
    val message: String,
    val id: String
)

// ----------------- Stripe Payment -----------------
data class PaymentRequest(
    val amount: Int // in cents
)

data class PaymentResponse(
    val clientSecret: String
)

interface ApiService {
    // Send email
    @Headers("Content-Type: application/json")
    @POST("/send-email")
    fun sendEmail(@Body request: EmailRequest): Call<EmailResponse>

    // Create Stripe payment intent
    @Headers("Content-Type: application/json")
    @POST("/create-payment-intent")
    fun createPaymentIntent(@Body request: PaymentRequest): Call<PaymentResponse>

    @GET("discharge-permit/pending-payment")
    fun getPendingDischargePermit(
        @Query("uid") userId: String
    ): Call<Boolean>
}
