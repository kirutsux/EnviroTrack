package com.ecocp.capstoneenvirotrack.model

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EmbPttApplication(
    var pttId: String = "",
    var generatorId: String = "",
    var generatorName: String? = null,
    var userId: String = "",
    var transportBookingId: String = "",
    var tsdBookingId: String = "",
    var amount: Double = 0.0,
    var generatorCertificateUrl: String = "",
    var transportPlanUrl: String = "",
    var paymentStatus: String = "",
    var status: String = "",
    var remarks: String = "",
    var submittedAt: Timestamp? = null // Store as Timestamp
) {
    // Convert Timestamp to Date
    fun submittedAtDate(): Date? = submittedAt?.toDate()

    // Nicely formatted string
    fun submittedAtFormatted(): String {
        submittedAt?.let {
            return try {
                val formatter =
                    SimpleDateFormat("MMMM d, yyyy 'at' h:mm:ss a 'UTC+8'", Locale.ENGLISH)
                formatter.format(it.toDate())
            } catch (e: Exception) {
                "N/A"
            }
        }
        return "N/A"
    }
}
