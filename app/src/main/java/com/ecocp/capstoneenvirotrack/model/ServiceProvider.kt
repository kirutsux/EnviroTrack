package com.ecocp.capstoneenvirotrack.model

data class ServiceProvider(
    val name: String = "",
    val companyName: String = "",
    val contactNumber: String = "",
    val email: String = "",
    val location: String = "",
    val role: String = "", // e.g. "Transporter" or "TSD Facility"
    val status: String = "" // Add this if you also want to show approved status
)
