package com.ecocp.capstoneenvirotrack.model

import com.google.firebase.Timestamp

data class TransporterBooking(
    val bookingId: String = "",
    val serviceProviderName: String = "",
    val serviceProviderCompany: String = "",
    val bookingStatus: String = "",
    val bookingDate: Timestamp? = null
)
