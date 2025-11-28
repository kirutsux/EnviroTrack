package com.ecocp.capstoneenvirotrack.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecocp.capstoneenvirotrack.api.OpenAiClient
import com.ecocp.capstoneenvirotrack.model.*
import kotlinx.coroutines.launch

class SmrViewModel : ViewModel() {

    // LiveData holding the entire SMR
    private val _smr = MutableLiveData(Smr())
    val smr: LiveData<Smr> get() = _smr

    // LiveData for analysis results from OpenAI
    private val _analysis = MutableLiveData<String>()
    val analysis: LiveData<String> get() = _analysis


    // Track progress per module (0-100%) using immutable Map for LiveData
    private val _moduleProgress = MutableLiveData<Map<String, Int>>(
        mapOf(
            "module1" to 0,
            "module2" to 0,
            "module3" to 0,
            "module4" to 0,
            "module5" to 0
        )
    )
    val moduleProgress: LiveData<Map<String, Int>> get() = _moduleProgress

    /** --- MODULE 1: GENERAL INFO --- **/
    fun updateGeneralInfo(info: GeneralInfo) {
        val current = _smr.value ?: Smr()
        _smr.value = current.copy(generalInfo = info)
        updateModuleProgress("module1", calculateGeneralInfoPercentage(info))
    }

    /** --- MODULE 2: HAZARDOUS WASTES --- **/
    fun addHazardousWaste(item: HazardousWaste) {
        val current = _smr.value ?: Smr()
        val updatedList = current.hazardousWastes.toMutableList().apply { add(item) }
        _smr.value = current.copy(hazardousWastes = updatedList)
        updateModuleProgress("module2", calculateListModulePercentage(updatedList))
    }

    fun updateHazardousWastes(wastes: List<HazardousWaste>) {
        val current = _smr.value ?: Smr()
        _smr.value = current.copy(hazardousWastes = wastes)
        updateModuleProgress("module2", calculateListModulePercentage(wastes))
    }

    fun removeHazardousWaste(item: HazardousWaste) {
        val current = _smr.value ?: Smr()
        val updatedList = current.hazardousWastes.toMutableList().apply { remove(item) }
        _smr.value = current.copy(hazardousWastes = updatedList)
        updateModuleProgress("module2", calculateListModulePercentage(updatedList))
    }

    /** --- MODULE 3: WATER POLLUTION --- **/
    fun addWaterPollutionRecord(record: WaterPollution) {
        val current = _smr.value ?: Smr()
        val updatedList = current.waterPollutionRecords.toMutableList().apply { add(record) }
        _smr.value = current.copy(waterPollutionRecords = updatedList)
        updateModuleProgress("module3", calculateListModulePercentage(updatedList))
    }

    fun updateWaterPollutionRecords(records: List<WaterPollution>) {
        val current = _smr.value ?: Smr()
        _smr.value = current.copy(waterPollutionRecords = records)
        updateModuleProgress("module3", calculateListModulePercentage(records))
    }

    fun clearWaterPollutionRecords() {
        val current = _smr.value ?: Smr()
        _smr.value = current.copy(waterPollutionRecords = emptyList())
        updateModuleProgress("module3", 0)
    }

    /** --- MODULE 4: AIR POLLUTION --- **/
    fun updateAirPollution(data: AirPollution) {
        val current = _smr.value ?: Smr()
        _smr.value = current.copy(airPollution = data)
        updateModuleProgress("module4", calculateAirPollutionPercentage(data))
    }

    /** --- MODULE 5: OTHERS --- **/
    fun updateOthers(data: Others) {
        val current = _smr.value ?: Smr()
        _smr.value = current.copy(others = data)
        updateModuleProgress("module5", calculateOthersPercentage(data))
    }

    /** --- UTILITIES --- **/
    fun clearSmr() {
        _smr.value = Smr()
        _moduleProgress.value = mapOf(
            "module1" to 0,
            "module2" to 0,
            "module3" to 0,
            "module4" to 0,
            "module5" to 0
        )
    }

    /** --- MARK AS SUBMITTED WITH UID --- **/
    fun markSubmitted(uid: String) {
        val current = _smr.value ?: Smr()
        _smr.value = current.copy(
            submittedAt = System.currentTimeMillis(),
            uid = uid
        )
    }

    /** --- UPDATE MODULE PROGRESS --- **/
    private fun updateModuleProgress(moduleKey: String, percentage: Int) {
        val currentMap = _moduleProgress.value?.toMutableMap() ?: mutableMapOf()
        currentMap[moduleKey] = percentage.coerceIn(0, 100)
        _moduleProgress.value = currentMap
    }

    /** --- CALCULATE PERCENTAGE COMPLETION --- **/
    private fun calculateGeneralInfoPercentage(info: GeneralInfo): Int {
        val fields = listOf(
            info.establishmentName, info.address, info.ownerName,
            info.phone, info.email, info.typeOfBusiness,
            info.ceoName, info.ceoPhone, info.ceoEmail,
            info.pcoName, info.pcoPhone, info.pcoEmail,
            info.pcoAccreditationNo, info.legalClassification
        )
        val filled = fields.count { !it.isNullOrEmpty() }
        return (filled.toFloat() / fields.size * 100).toInt()
    }

    private fun calculateListModulePercentage(list: List<*>): Int {
        // Simple assumption: each module expects at least 5 entries for 100% completion
        val totalExpected = 5
        val filled = list.size.coerceAtMost(totalExpected)
        return (filled.toFloat() / totalExpected * 100).toInt()
    }

    private fun calculateAirPollutionPercentage(module: AirPollution): Int {
        val fields = listOf(
            module.processEquipment, module.location, module.emissionDescription
        )
        val filled = fields.count { !it.isNullOrEmpty() }
        return (filled.toFloat() / fields.size * 100).toInt()
    }

    private fun calculateOthersPercentage(module: Others): Int {
        val fields = listOf(
            module.accidentDate, module.accidentArea, module.trainingDescription
        )
        val filled = fields.count { !it.isNullOrEmpty() }
        return (filled.toFloat() / fields.size * 100).toInt()
    }

    private val _aiAnalysis = MutableLiveData<String>()
    val aiAnalysis: LiveData<String> get() = _aiAnalysis

    fun analyzeSummary(summaryText: String) {
        viewModelScope.launch {
            val request = ChatRequest(
                model = "gpt-3.5-turbo",
                messages = listOf(
                    ApiMessage(
                        role = "system",
                        content = "You are an environmental compliance AI. Analyze SMR summaries."
                    ),
                    ApiMessage(
                        role = "user",
                        content = "Analyze the following SMR summary:\n$summaryText"
                    )
                )
            )
            try {
                val response = OpenAiClient.instance.getChatCompletion(request)

                val content = response
                    .choices
                    .firstOrNull()
                    ?.message
                    ?.content
                    ?: "No analysis was generated."

                _analysis.value = content
            } catch (e: Exception) {
                _analysis.value = "Analysis failed: ${e.message}"
            }
        }
    }
}
