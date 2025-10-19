package com.ecocp.capstoneenvirotrack.model

data class Crs(
    val docId: String,               // Firestore document id
    val companyName: String,
    val address: String,
    val status: String = "Pending",
    val dateSubmitted: String
)
