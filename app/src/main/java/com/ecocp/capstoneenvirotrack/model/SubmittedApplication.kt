package com.ecocp.capstoneenvirotrack.model

import com.google.firebase.Timestamp

data class SubmittedApplication(
    val id: String = "",
    val companyName: String = "",
    val companyAddress: String = "",
    val pcoName: String = "",
    val pcoAccreditation: String = "",
    val contactNumber: String = "",
    val email: String = "",
    val bodyOfWater: String = "",
    val sourceWastewater: String = "",
    val volume: String = "",
    val treatmentMethod: String = "",
    val operationStartDate: String = "",
    val fileLinks: List<String> = emptyList(),
    val status: String = "Pending",
    val paymentInfo: String = "",
    val timestamp: String = "",
    val applicationType: String = "",
    val issueDate: Timestamp? = null,   // ✅ Added
    val expiryDate: Timestamp? = null // ✅ Added
)
