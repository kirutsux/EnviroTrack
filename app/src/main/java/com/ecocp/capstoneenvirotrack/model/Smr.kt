package com.ecocp.capstoneenvirotrack.model

data class Smr(
    val generalInfo: GeneralInfo = GeneralInfo(),
    val hazardousWastes: List<HazardousWaste> = emptyList(),
    val waterPollutionRecords: List<WaterPollution> = emptyList(),
    val airPollution: AirPollution = AirPollution(),
    val others: Others = Others(),
    val submittedAt: Long? = null,
    val uid: String? = null, // just store the Firebase UID
    var id: String? = null,
    val status: String = "Pending",
    val fileUrls: List<String> = emptyList()
)

