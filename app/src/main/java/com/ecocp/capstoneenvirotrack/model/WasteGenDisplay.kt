package com.ecocp.capstoneenvirotrack.model

data class WasteGenDisplay(
    val id: String = "",
    val companyName: String = "",
    val embRegNo: String = "",
    val status: String = "",
    val timestamp: String = "",
    val wasteList: List<WasteItem> = emptyList()
)

data class WasteItem(
    val wasteName: String = "",
    val quantity: String = "",
    val wasteCode: String = ""
)
