package com.ecocp.capstoneenvirotrack.model

data class Crs(
    val docId: String,
    val companyName: String,
    val companyType: String,
    val tinNumber: String,
    val ceoName: String,
    val ceoContact: String,
    val natureOfBusiness: String,
    val psicNo: String,
    val industryDescriptor: String,
    val address: String,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val repName: String? = null,
    val repPosition: String? = null,
    val repContact: String? = null,
    val repEmail: String? = null,
    val fileUrls: List<String> = emptyList(),
    val status: String = "Pending",
    val dateSubmitted: String
)