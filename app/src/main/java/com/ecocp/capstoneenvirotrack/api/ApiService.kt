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

// ----------------- Push Notification -----------------
data class SendNotificationRequest(
    val receiverId: String,
    val title: String,
    val message: String
)

// ----------------- Notification: PCO Submission -----------------
data class PcoSendNotificationRequest(
    val receiverId: String,   // PCO UID
    val module: String,       // PTO, DISCHARGE, CNC, SMR, HMS, CRS, PCO
    val documentId: String    // ID of submitted record
)

// ----------------- Notification: EMB Status Update -----------------
data class UpdateStatusRequest(
    val applicationId: String,
    val newStatus: String,    // approved / rejected
    val pcoId: String,        // Notify PCO
    val embId: String,        // Notify EMB
    val module: String,       // PTO / CNC / SMR etc.
    val feedback: String? = null
)

data class SendExpiryNotificationRequest(
    val userId: String,    // PCO UID
    val type: String,      // PTO / DISCHARGE / PCO etc.
    val daysLeft: Long     // Number of days left until expiry
)

// ----------------- SP Booking Notifications -----------------
data class NotifyBookingCreatedRequest(
    val bookingId: String
)
data class NotifyBookingStatusRequest(
    val receiverId: String,      // PCO ID
    val bookingId: String,
    val status: String,          // accepted / rejected
    val role: String             // transporter / tsd
)

data class NotifyPttUploadRequest(
    val receiverId: String,      // transporter ID
    val bookingId: String
)

// ----------------- Notification: Service Provider Status Update -----------
data class UpdateWasteStatusDelivered(
    val pcoId: String,
    val bookingId: String
)
data class UpdateWasteInTransit(
    val pcoId: String,
    val bookingId: String,
    val tsdId: String
)
data class UpdateWasteTreated(
    val pcoId: String,
    val bookingId: String
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

    // ==========================================================
//        SERVICE PROVIDER BOOKING NOTIFICATIONS
// ==========================================================

    // 1️⃣ PCO ➜ Transporter: New booking created
    @Headers("Content-Type: application/json")
    @POST("/notify-transporter-booking")
    fun notifyBookingCreated(
        @Body request: NotifyBookingCreatedRequest
    ): Call<Void>
    // 2️⃣ Transporter ➜ PCO: Accept/Reject booking
    @Headers("Content-Type: application/json")
    @POST("/notify/sp/booking-status")
    fun notifyBookingStatus(
        @Body request: NotifyBookingStatusRequest
    ): Call<Void>

    // 3️⃣ PCO ➜ TSD: New booking created
    @Headers("Content-Type: application/json")
    @POST("/notify-tsd-booking")
    fun notifyTsdBookingCreated(
        @Body request: NotifyBookingCreatedRequest
    ): Call<Void>

    // 4️⃣ TSD ➜ PCO: Accept/Reject booking
    @Headers("Content-Type: application/json")
    @POST("/notify/sp/tsd-booking-status")
    fun notifyTsdBookingStatus(
        @Body request: NotifyBookingStatusRequest
    ): Call<Void>

    // ==========================================================
//        TRANSPORTER / PCO SPECIFIC NOTIFICATIONS
// ==========================================================
    // 5️⃣ PCO ➜ Transporter: PTT certificate uploaded
    @Headers("Content-Type: application/json")
    @POST("/notify/sp/ptt-uploaded")
    fun notifyPttUploaded(
        @Body request: NotifyPttUploadRequest
    ): Call<Void>


    // ----------------- UPDATE STATUS Transporter -> PCO (Marked Delivered) -----------------------
    @Headers("Content-Type: application/json")
    @POST("/notify-pco-delivered")
    fun updateWasteStatusDelivered(
        @Body request: UpdateWasteStatusDelivered
    ): Call<Void>
    @Headers("Content-Type:application/json")
    @POST("/notify-pco-in-transit")
    fun updateWasteInTransit(
        @Body request: UpdateWasteInTransit
    ): Call<Void>
    @Headers("Content-Type:application/json")
    @POST("/notify-pco-treatment-finished")
    fun updateWasteTreated(
        @Body request: UpdateWasteTreated
    ): Call<Void>

    // ----------------- Push Notification -----------------
    @Headers("Content-Type: application/json")
    @POST("/send-notification")
    fun sendNotification(@Body request: SendNotificationRequest): Call<Void>

    // =============== NOTIFICATIONS (PCO → EMB + PCO) ===============
    @Headers("Content-Type: application/json")
    @POST("/send-notification")
    fun sendPcoSubmissionNotification(
        @Body request: PcoSendNotificationRequest
    ): Call<Void>

    // =============== UPDATE STATUS (EMB → PCO + EMB) ===============
    @Headers("Content-Type: application/json")
    @POST("/update-status")
    fun updateStatus(
        @Body request: UpdateStatusRequest
    ): Call<Void>

    // ----------------- Push Notification: Expiry Alert -----------------
    @Headers("Content-Type: application/json")
    @POST("/send-expiry-notification")
    fun sendExpiryNotification(
        @Body request: SendExpiryNotificationRequest
    ): Call<Void>

}
