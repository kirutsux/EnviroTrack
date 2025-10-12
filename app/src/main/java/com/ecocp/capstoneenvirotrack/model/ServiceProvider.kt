package com.ecocp.capstoneenvirotrack.model

data class ServiceProvider(
    val name: String = "",
    val companyName: String = "",
    val contactNumber: String = "",
    val email: String = "",
    val address: String = "",
    val type: String = "" // e.g. "Transporter" or "TSD Facility"
)