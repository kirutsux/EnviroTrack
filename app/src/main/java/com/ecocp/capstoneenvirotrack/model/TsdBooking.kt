package com.ecocp.capstoneenvirotrack.model

data class TsdBooking(
    val id: String = "",
    val bookingId: String? = null,
    val certificateUrl: String? = null,
    val contactNumber: String? = null,
    val dateCreated: String? = null, // map Firestore timestamp to ISO if needed
    val facilityId: String? = null,
    val facilityName: String? = null,
    val location: String? = null,
    val preferredDate: String? = null,
    val previousRecordUrl: String? = null,
    val quantity: Double? = null,
    val rate: Double? = null,
    val status: String? = null,
    val totalPayment: Double? = null,
    val treatmentInfo: String? = null,
    val userId: String? = null
)
