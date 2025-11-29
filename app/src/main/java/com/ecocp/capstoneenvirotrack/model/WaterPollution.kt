package com.ecocp.capstoneenvirotrack.model

data class WaterPollution(
    val domesticWastewater: Double = 0.0,
    val processWastewater: Double = 0.0,
    val coolingWater: String = "",
    val otherSource: String = "",
    val washEquipment: String = "",
    val washFloor: String = "",
    val employees: Int = 0,
    val costEmployees: String = "",
    val utilityCost: String = "",
    val newInvestmentCost: String = "",
    val outletNo: Int = 0,
    val outletLocation: String = "",
    val waterBody: String = "",
    val date1: String = "",
    val flow1: String = "",
    val bod1: String = "",
    val tss1: String = "",
    val color1: String = "",
    val ph1: String = "",
    val oilGrease1: String = "",
    val tempRise1: String = "",
    val do1: String = ""
)
