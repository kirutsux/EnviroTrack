package com.ecocp.capstoneenvirotrack.model

import com.google.firebase.Timestamp

data class PCO(
    val appId: String = "",
    val appName: String = "",
    val applicant: String = "",
    val forwardedTo: String = "",
    val updatedDate: String = "",
    val issueDate: Timestamp? = null,   // ✅ Changed from Long? to Timestamp?
    val expiryDate: Timestamp? = null,   // ✅ Changed from Long? to Timestamp?
    val status: String = ""
)
