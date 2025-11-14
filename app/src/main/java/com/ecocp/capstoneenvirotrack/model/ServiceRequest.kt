package com.ecocp.capstoneenvirotrack.model

data class ServiceRequest(
    val id: String = "",
    val clientName: String = "",
    val companyName: String = "",
    val serviceTitle: String = "",
    val status: String = "",
    val compliance: String = "",
    val imageUrl: String = "" // Optional if you have client pictures
)