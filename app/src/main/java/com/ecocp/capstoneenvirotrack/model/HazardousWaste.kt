package com.ecocp.capstoneenvirotrack.model

data class HazardousWaste(
    val commonName: String = "",
    val casNo: String = "",
    val tradeName: String = "",
    val hwNo: String = "",
    val hwClass: String = "",
    val hwGenerated: String = "",
    val storageMethod: String = "",
    val transporter: String = "",
    val treater: String = "",
    val disposalMethod: String = ""
)
