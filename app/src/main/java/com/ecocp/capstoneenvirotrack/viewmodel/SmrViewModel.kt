package com.ecocp.capstoneenvirotrack.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.ecocp.capstoneenvirotrack.api.OpenAiClient
import com.ecocp.capstoneenvirotrack.model.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

// --------------------------------------------
// DATASTORE EXTENSION
// --------------------------------------------
private val Application.smrDataStore by preferencesDataStore("smr_ai_cache")

class SmrViewModel(private val app: Application) : AndroidViewModel(app) {
    init {
        Log.d("SmrViewModel", "SmrViewModel CREATED — instance=${System.identityHashCode(this)}")
    }
    // SMR LiveData
    private val _smr = MutableLiveData(Smr())
    val smr: LiveData<Smr> get() = _smr

    // AI Analysis
    private val _aiAnalysis = MutableLiveData<String>()
    val aiAnalysis: LiveData<String> get() = _aiAnalysis

    // Track Module Progress
    private val _moduleProgress = MutableLiveData(
        mapOf(
            "module1" to 0,
            "module2" to 0,
            "module3" to 0,
            "module4" to 0,
            "module5" to 0
        )
    )
    val moduleProgress: LiveData<Map<String, Int>> get() = _moduleProgress

    // ---------------------------------------------------------
    // DATASTORE KEYS
    // ---------------------------------------------------------
    private val KEY_LAST_PROMPT_HASH = stringPreferencesKey("last_prompt_hash")
    private val KEY_LAST_AI_OUTPUT = stringPreferencesKey("last_ai_output")

    // ---------------------------------------------------------
    // SAVE TO DATASTORE
    // ---------------------------------------------------------
    private suspend fun saveToCache(promptHash: String, aiOutput: String) {
        app.smrDataStore.edit { prefs ->
            prefs[KEY_LAST_PROMPT_HASH] = promptHash
            prefs[KEY_LAST_AI_OUTPUT] = aiOutput
        }
    }

    // ---------------------------------------------------------
    // LOAD FROM CACHE
    // ---------------------------------------------------------
    private suspend fun loadCached(): Pair<String?, String?> {
        val prefs = app.smrDataStore.data.first()
        return Pair(
            prefs[KEY_LAST_PROMPT_HASH],
            prefs[KEY_LAST_AI_OUTPUT]
        )
    }

    // =========================================================
    // AI ANALYSIS WITH TIMEOUT + CACHE
    // =========================================================
    fun analyzeSummary(prompt: String) {
        viewModelScope.launch {

            val promptHash = prompt.hashCode().toString()

            // LOAD CACHE FIRST
            val (cachedHash, cachedOutput) = loadCached()

            // ---------------------------------------------
            // USE CACHE IF PROMPT DIDN’T CHANGE
            // ---------------------------------------------
            if (cachedHash == promptHash && !cachedOutput.isNullOrEmpty()) {
                _aiAnalysis.value = cachedOutput
                return@launch
            }

            // ---------------------------------------------
            // OTHERWISE → NEW AI REQUEST WITH TIMEOUT
            // ---------------------------------------------
            try {
                val request = ChatRequest(
                    model = "gpt-4.1-mini",
                    messages = listOf(
                        ApiMessage(
                            role = "system",
                            content = "You are an environmental compliance AI assistant..."
                        ),
                        ApiMessage(
                            role = "user",
                            content = prompt
                        )
                    )
                )

                val response = withTimeout(10_000) {
                    OpenAiClient.instance.getChatCompletion(request)
                }

                val output = response.choices.firstOrNull()?.message?.content
                    ?: "No analysis generated."

                _aiAnalysis.value = output
                saveToCache(promptHash, output)

            } catch (e: TimeoutCancellationException) {
                if (!cachedOutput.isNullOrEmpty()) {
                    _aiAnalysis.value =
                        "⚠️ AI request timed out. Loaded cached analysis.\n\n$cachedOutput"
                } else {
                    _aiAnalysis.value =
                        "⚠️ AI request timed out and no cached analysis is available."
                }

            } catch (e: Exception) {
                _aiAnalysis.value = "AI analysis failed: ${e.message}"
            }
        }
    }

    // =========================================================
    // SMR MODULE UPDATES
    // =========================================================

    fun updateGeneralInfo(info: GeneralInfo) {
        val current = _smr.value ?: Smr()
        _smr.value = current.copy(generalInfo = info)
        updateModuleProgress("module1", calculateGeneralInfoPercentage(info))
    }

    // ---------------- MODULE 2 ----------------
    fun addHazardousWaste(item: HazardousWaste) {
        val current = _smr.value ?: Smr()
        val updatedList = current.hazardousWastes.toMutableList().apply { add(item) }
        _smr.value = current.copy(hazardousWastes = updatedList)
        updateModuleProgress("module2", calculateListModulePercentage(updatedList))
    }

    fun updateHazardousWastes(list: List<HazardousWaste>) {
        Log.d("SmrViewModel", "updateHazardousWastes called (vm=${System.identityHashCode(this)}) size=${list.size}")
        val current = _smr.value ?: Smr()
        _smr.value = current.copy(hazardousWastes = list)
        updateModuleProgress("module2", calculateListModulePercentage(list))
        Log.d("SmrViewModel", "after updateHazardousWastes smr.hazardousWastes.size=${_smr.value?.hazardousWastes?.size}")
    }

    fun removeHazardousWaste(item: HazardousWaste) {
        val current = _smr.value ?: Smr()
        val updatedList = current.hazardousWastes.toMutableList().apply { remove(item) }
        _smr.value = current.copy(hazardousWastes = updatedList)
        updateModuleProgress("module2", calculateListModulePercentage(updatedList))
    }

    // ---------------- MODULE 3 ----------------
    fun addWaterPollutionRecord(record: WaterPollution) {
        val current = _smr.value ?: Smr()
        val updatedList = current.waterPollutionRecords.toMutableList().apply { add(record) }
        _smr.value = current.copy(waterPollutionRecords = updatedList)
        updateModuleProgress("module3", calculateListModulePercentage(updatedList))
    }

    fun updateWaterPollutionRecords(list: List<WaterPollution>) {
        Log.d("SmrViewModel", "updateWaterPollutionRecords called (vm=${System.identityHashCode(this)}) size=${list.size}")
        val current = _smr.value ?: Smr()
        _smr.value = current.copy(waterPollutionRecords = list)
        updateModuleProgress("module3", calculateListModulePercentage(list))
        Log.d("SmrViewModel", "after updateWaterPollutionRecords smr.waterPollutionRecords.size=${_smr.value?.waterPollutionRecords?.size}")
    }

    fun clearWaterPollutionRecords() {
        val current = _smr.value ?: Smr()
        _smr.value = current.copy(waterPollutionRecords = emptyList())
        updateModuleProgress("module3", 0)
    }

    // ---------------- MODULE 4 ----------------
    fun updateAirPollution(data: AirPollution) {
        val current = _smr.value ?: Smr()
        _smr.value = current.copy(airPollution = data)
        updateModuleProgress("module4", calculateAirPollutionPercentage(data))
    }

    // ---------------- MODULE 5 ----------------
    fun updateOthers(data: Others) {
        val current = _smr.value ?: Smr()
        _smr.value = current.copy(others = data)
        updateModuleProgress("module5", calculateOthersPercentage(data))
    }

    // ---------------- CLEAR ALL ----------------
    fun clearSmr() {
        _smr.value = Smr()
        _moduleProgress.value = mapOf(
            "module1" to 0, "module2" to 0, "module3" to 0,
            "module4" to 0, "module5" to 0
        )
    }

    // =========================================================
    // UTILITY FUNCTIONS
    // =========================================================

    private fun updateModuleProgress(key: String, percent: Int) {
        val map = _moduleProgress.value!!.toMutableMap()
        map[key] = percent.coerceIn(0, 100)
        _moduleProgress.value = map
    }

    private fun calculateGeneralInfoPercentage(info: GeneralInfo): Int {
        val fields = listOf(
            info.establishmentName, info.address, info.ownerName,
            info.phone, info.email, info.typeOfBusiness,
            info.ceoName, info.ceoPhone, info.ceoEmail,
            info.pcoName, info.pcoPhone, info.pcoEmail,
            info.pcoAccreditationNo, info.legalClassification
        )
        val filled = fields.count { !it.isNullOrEmpty() }
        return (filled / fields.size.toFloat() * 100).toInt()
    }

    private fun calculateListModulePercentage(list: List<*>): Int =
        ((list.size.coerceAtMost(5) / 5f) * 100).toInt()

    private fun calculateAirPollutionPercentage(a: AirPollution): Int {
        val fields = listOf(a.processEquipment, a.location, a.emissionDescription)
        val filled = fields.count { !it.isNullOrEmpty() }
        return (filled / fields.size.toFloat() * 100).toInt()
    }

    private fun calculateOthersPercentage(o: Others): Int {
        val fields = listOf(o.accidentDate, o.accidentArea, o.trainingDescription)
        val filled = fields.count { !it.isNullOrEmpty() }
        return (filled / fields.size.toFloat() * 100).toInt()
    }
}
