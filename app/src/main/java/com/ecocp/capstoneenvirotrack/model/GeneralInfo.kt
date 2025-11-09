package com.ecocp.capstoneenvirotrack.model

data class GeneralInfo(
    val establishmentName: String = "",
    val address: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val email: String = "",
    val typeOfBusiness: String = "",

    // CEO / Managing Head fields
    val ceoName: String? = null,
    val ceoPhone: String? = null,
    val ceoEmail: String? = null,

    // PCO fields
    val pcoName: String = "",
    val pcoPhone: String = "",
    val pcoEmail: String = "",
    val pcoAccreditationNo: String = "",
    val legalClassification: String = ""
)
