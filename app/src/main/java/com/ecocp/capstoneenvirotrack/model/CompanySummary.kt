package com.ecocp.capstoneenvirotrack.model

import java.util.*

data class CompanySummary(
    val docId: String,
    val companyName: String,
    val userId: String,
    val latestCncStatus: String? = null,
    val latestPcoStatus: String? = null,
    val latestPtoExpiry: Date? = null,
    val latestDpExpiry: Date? = null,
    val pcoExpiry: Date? = null,
    val complianceScore: Int = 0,
    val overallStatus: String = "Unknown"
)
