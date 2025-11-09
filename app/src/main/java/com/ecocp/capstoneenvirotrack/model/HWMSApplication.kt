package com.ecocp.capstoneenvirotrack.model

data class HWMSApplication(
    val id: String = "",
    val wasteType: String = "",
    val quantity: String = "",
    val unit: String = "",
    val storageLocation: String = "",
    val dateGenerated: String = "",
    val transporterName: String = "",
    val tsdFacilityName: String = "",
    val permitNumber: String? = null,
    val paymentStatus: String? = null,
    val status: String = "Pending"
)
