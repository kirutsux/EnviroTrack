package com.ecocp.capstoneenvirotrack.model

import com.google.firebase.Timestamp

data class EmbOpmsApplication(
    val applicationId: String,
    val applicationType: String, // "Permit to Operate" or "Discharge Permit"

    // --- Common fields ---
    val companyName: String? = null,
    val status: String? = "Pending",
    val submittedTimestamp: Timestamp? = null,
    val issueDate: Timestamp? = null,   // ✅ Added
    val expiryDate: Timestamp? = null,  // ✅ Added

    // --- PTO-specific fields ---
    val ownerName: String? = null,
    val establishmentName: String? = null,
    val mailingAddress: String? = null,
    val plantAddress: String? = null,
    val tin: String? = null,
    val ownershipType: String? = null,
    val natureOfBusiness: String? = null,
    val pcoName: String? = null,
    val pcoAccreditation: String? = null,
    val operatingHours: String? = null,
    val totalEmployees: String? = null,
    val landArea: String? = null,
    val equipmentName: String? = null,
    val fuelType: String? = null,
    val emissions: String? = null,

    // --- DP-specific fields ---
    val companyAddress: String? = null,
    val pcoAccreditationNumber: String? = null,
    val receivingBody: String? = null,
    val dischargeVolume: String? = null,
    val dischargeMethod: String? = null,
    val uploadedFiles: String? = null
)
