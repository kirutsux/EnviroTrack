package com.ecocp.capstoneenvirotrack.model

import com.google.firebase.Timestamp

data class CrsApplication(
    val applicationId: String = "",
    val companyName: String = "",
    val companyType: String = "",
    val industryDescriptor: String = "",
    val natureOfBusiness: String = "",
    val status: String = "",
    val dateSubmitted: Timestamp? = null,
)
