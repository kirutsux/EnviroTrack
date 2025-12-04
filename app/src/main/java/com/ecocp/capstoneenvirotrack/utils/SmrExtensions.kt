package com.ecocp.capstoneenvirotrack.utils

import com.ecocp.capstoneenvirotrack.model.*

fun GeneralInfo.generalInfoText() = """
        Establishment: $establishmentName
        Address: $address
        Owner: $ownerName
        Phone: $phone
        Email: $email
        Type of Business: $typeOfBusiness
        CEO Name: $ceoName
        CEO Phone: $ceoPhone
        CEO Email: $ceoEmail
        PCO Name: $pcoName
        PCO Phone: $pcoPhone
        PCO Email: $pcoEmail
        PCO Accreditation No.: $pcoAccreditationNo
        Legal Classification: $legalClassification
    """.trimIndent()

fun List<HazardousWaste>.hazardousWasteText(): String =
    if (isEmpty()) "No hazardous waste entries."
    else mapIndexed { i, hw ->
        """
        • Entry ${i + 1}:
          Name: ${hw.commonName}
          CAS No: ${hw.casNo}
          Trade Name: ${hw.tradeName}
          HW No: ${hw.hwNo}
          HW Class: ${hw.hwClass}
          Generated: ${hw.hwGenerated}
          Storage: ${hw.storageMethod}
          Transporter: ${hw.transporter}
          Treater: ${hw.treater}
          Disposal Method: ${hw.disposalMethod}
    """.trimIndent()
    }.joinToString("\n\n")

fun List<WaterPollution>.waterPollutionText(): String =
    if (isEmpty()) "No water pollution entries."
    else mapIndexed { i, wp ->
        """
        • Entry ${i + 1}:
          Domestic Wastewater: ${wp.domesticWastewater}
          Process Wastewater: ${wp.processWastewater}
          Cooling Water: ${wp.coolingWater}
          Other Source: ${wp.otherSource}
          Wash Equipment: ${wp.washEquipment}
          Wash Floor: ${wp.washFloor}
          Employees: ${wp.employees}
          Cost of Employees: ${wp.costEmployees}
          Utility Cost: ${wp.utilityCost}
          New Investment Cost: ${wp.newInvestmentCost}
          Outlet No: ${wp.outletNo}
          Outlet Location: ${wp.outletLocation}
          Water Body: ${wp.waterBody}
          Date: ${wp.date1}
          Flow: ${wp.flow1}
          BOD: ${wp.bod1}
          TSS: ${wp.tss1}
          Color: ${wp.color1}
          pH: ${wp.ph1}
          Oil & Grease: ${wp.oilGrease1}
          Temperature Rise: ${wp.tempRise1}
          DO: ${wp.do1}
    """.trimIndent()
    }.joinToString("\n\n")

fun AirPollution.airPollutionText() = """
        Equipment: $processEquipment
        Location: $location
        Hours Operation: $hoursOperation
        Fuel Equipment: $fuelEquipment
        Fuel Used: $fuelUsed
        Fuel Quantity: $fuelQuantity
        Fuel Hours: $fuelHours
        PCF Name: $pcfName
        PCF Location: $pcfLocation
        PCF Hours: $pcfHours
        Total Electricity: $totalElectricity
        Overhead Cost: $overheadCost
        Emission Description: $emissionDescription
        Emission Date: $emissionDate
        Flow Rate: $flowRate
        CO: $co
        NOx: $nox
        Particulates: $particulates
    """.trimIndent()

fun Others.othersText() = """
        Accident Date: $accidentDate
        Accident Area: $accidentArea
        Findings: $findings
        Actions Taken: $actionsTaken
        Remarks: $remarks
        Training Date: $trainingDate
        Training Description: $trainingDescription
        Personnel Trained: $personnelTrained
    """.trimIndent()