package com.ecocp.capstoneenvirotrack.model

data class PttApplication(
    val generatorId: String,
    val transportBookingId: String,
    val tsdBookingId: String,
    val remarks: String = "None",
    val status: String = "Pending Review",
    val timestamp: Long = System.currentTimeMillis(),
    val generatorCertificate: String? = null,
    val transportPlan: String? = null
)
