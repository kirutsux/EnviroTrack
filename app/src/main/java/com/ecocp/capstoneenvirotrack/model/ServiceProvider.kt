package com.ecocp.capstoneenvirotrack.model

import com.google.firebase.Timestamp

data class ServiceProvider(
    val uid: String = "",
    val name: String = "",
    val companyName: String = "",
    val contactNumber: String = "",
    val email: String = "",
    val location: String = "",
    val role: String = "", // e.g., "Transporter" or "TSD Facility"
    val status: String = "", // e.g., "approved", "pending"
    val profileImageUrl: String = "",
    val mustChangePassword: Boolean = false,
    val createdAt: Timestamp? = null
)
