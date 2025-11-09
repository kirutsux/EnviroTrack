package com.ecocp.capstoneenvirotrack.model

data class AirPollution(
    val processEquipment: String = "",
    val location: String = "",
    val hoursOperation: String = "",
    val fuelEquipment: String = "",
    val fuelUsed: String = "",
    val fuelQuantity: String = "",
    val fuelHours: String = "",
    val pcfName: String = "",
    val pcfLocation: String = "",
    val pcfHours: String = "",
    val totalElectricity: String = "",
    val overheadCost: String = "",
    val emissionDescription: String = "",
    val emissionDate: String = "",
    val flowRate: String = "",
    val co: String = "",
    val nox: String = "",
    val particulates: String = ""
)
