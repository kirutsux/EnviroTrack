package com.ecocp.capstoneenvirotrack.model

data class TSDFacility(
    val id: String = "",
    val name: String = "",
    val companyName: String = "",
    val contactNumber: String = "",
    val location: String = "",
    val wasteType: String = "",      // Optional; shown as "Not specified" if missing
    val rate: Double = 0.0,          // Optional; defaults to 0.0 if missing
    val capacity: Int = 0,           // ✅ Added for flexibility (optional)
    val profileImageUrl: String = "", // ✅ Added for UI display (optional)
)
