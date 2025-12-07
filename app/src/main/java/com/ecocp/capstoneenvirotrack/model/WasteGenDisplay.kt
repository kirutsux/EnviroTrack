package com.ecocp.capstoneenvirotrack.model

import com.google.firebase.Timestamp

data class WasteGenDisplay(
    val id: String = "",
    val companyName: String = "",
    val embRegNo: String = "",
    val status: String = "",
    val timestamp: Timestamp? = null,   // <- Firebase Timestamp
    val wasteList: List<WasteItem> = emptyList(),
    var transportBookingId: String? = null,
    var transportStatus: String? = null
)

data class WasteItem(
    val wasteName: String = "",
    val quantity: String = "",
    val wasteCode: String = ""
)
