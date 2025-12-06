package com.ecocp.capstoneenvirotrack.model

import java.util.Date

data class BookingUiModel(
    val bookingId: String = "",
    val actorId: String = "",          // facilityId for TSD or transporterId for Transporter
    val status: String = "",
    val statusUpdatedAt: Date? = null,
    val certificateUrl: String? = null,
    val isTsdView: Boolean = false
)
