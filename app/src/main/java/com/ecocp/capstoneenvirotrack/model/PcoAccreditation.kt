package com.ecocp.capstoneenvirotrack.model

import com.google.firebase.Timestamp


data class PcoAccreditation(
    val applicationId: String = "",
    val fullName: String? = null,
    val companyAffiliation: String? = null,
    val positionDesignation: String? = null,
    val status: String? = null,
    val submittedTimestamp: Timestamp? = null,
    val issueDate: Timestamp? = null,   // ✅ Added
    val expiryDate: Timestamp? = null // ✅ Added
)
