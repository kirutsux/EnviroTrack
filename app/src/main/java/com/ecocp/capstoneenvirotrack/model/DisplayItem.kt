package com.ecocp.capstoneenvirotrack.model

data class DisplayItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val transporter: String,
    val tsdFacility: String,
    val permitNo: String,
    val paymentStatus: String,
    val status: String,
    val rawMap: Map<String, Any>
)
