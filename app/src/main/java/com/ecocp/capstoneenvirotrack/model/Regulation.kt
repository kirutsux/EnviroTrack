package com.ecocp.capstoneenvirotrack.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Regulation(
    val id: String = "",
    val regulationName: String = "",
    val republicAct: String = "",
    val description: String = "",
    val complianceRequirements: String = "",
    val fileURL: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    val createdBy: String = ""
)