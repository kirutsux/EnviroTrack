package com.ecocp.capstoneenvirotrack.api

import com.ecocp.capstoneenvirotrack.model.TsdBooking
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
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

// ----------------- AI FAQ Assistant -----------------
data class AskRequest(
    val question: String,
    val module: String
)

data class AskResponse(
    val answer: String
)

// ----------------- TSD request payloads -----------------
data class AcceptRequest(
    val remarks: String? = null,
    val scheduledDate: String? = null
)

data class RejectRequest(
    val reason: String
)

data class ReceiveRequest(
    val quantity: Double,
    val remarks: String? = null
)

data class TreatRequest(
    val notes: String? = null,
    val treatedQuantity: Double? = null
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

    // Ask AI FAQ Assistant
    @Headers("Content-Type: application/json")
    @POST("/ask")
    fun askAI(@Body request: AskRequest): Call<AskResponse>

    @GET("discharge-permit/pending-payment")
    fun getPendingDischargePermit(
        @Query("uid") userId: String
    ): Call<Boolean>

    // =================== NEW TSD ENDPOINTS ===================

    @GET("api/tsd/bookings")
    fun getTsdBookings(
        @Header("Authorization") auth: String
    ): Call<List<TsdBooking>>

    @Headers("Content-Type: application/json")
    @POST("api/tsd/bookings/{id}/accept")
    fun acceptTsdBooking(
        @Header("Authorization") auth: String,
        @Path("id") bookingId: String,
        @Body body: Map<String, String>
    ): Call<Void>

    @Headers("Content-Type: application/json")
    @POST("api/tsd/bookings/{id}/reject")
    fun rejectTsdBooking(
        @Header("Authorization") auth: String,
        @Path("id") bookingId: String,
        @Body body: Map<String, String>
    ): Call<Void>

    @Headers("Content-Type: application/json")
    @POST("api/tsd/bookings/{id}/receive")
    fun receiveTsdBooking(
        @Header("Authorization") auth: String,
        @Path("id") bookingId: String,
        @Body body: Map<String, Double>
    ): Call<Void>

    @Headers("Content-Type: application/json")
    @POST("api/tsd/bookings/{id}/treat")
    fun treatTsdBooking(
        @Header("Authorization") auth: String,
        @Path("id") bookingId: String,
        @Body body: Map<String, String>
    ): Call<Void>
}
