package com.ecocp.capstoneenvirotrack.model

data class HWMSApplication(

    // EXISTING FIELDS
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
    val status: String = "",
    val embStatus: String = "",

    // NEW: REQUIRED FOR TRANSPORT STEP + PTT
    val generatorId: String = "",             // For transport booking
    val transportBookingId: String = "",      // For PTT Step 2
    val tsdBookingId: String = ""             // For PTT Step 3
)
