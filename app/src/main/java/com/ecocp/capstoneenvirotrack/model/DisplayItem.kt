package com.ecocp.capstoneenvirotrack.model

data class DisplayItem(
    val id: String,
    val title: String,           // e.g., wasteType or facilityName or "PTT Application"
    val subtitle: String,        // e.g., quantity / details
    val transporter: String,     // transporter name (if any)
    val tsdFacility: String,     // tsd facility name (if any)
    val permitNo: String,        // permit number or bookingId
    val paymentStatus: String,   // Paid / Unpaid / Pending
    val status: String,          // Pending / Confirmed / etc.
    val rawMap: Map<String, Any> // original document data if you need it
)
