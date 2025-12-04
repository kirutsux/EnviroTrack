package com.ecocp.capstoneenvirotrack.viewmodel

//import androidx.datastore.preferences.core.*
//import androidx.datastore.preferences.preferencesDataStore
//import com.ecocp.capstoneenvirotrack.api.OpenAiClient
import android.app.Application
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.ecocp.capstoneenvirotrack.MyApplication
import com.ecocp.capstoneenvirotrack.model.AirPollution
import com.ecocp.capstoneenvirotrack.model.GeneralInfo
import com.ecocp.capstoneenvirotrack.model.HazardousWaste
import com.ecocp.capstoneenvirotrack.model.Others
import com.ecocp.capstoneenvirotrack.model.Smr
import com.ecocp.capstoneenvirotrack.model.WaterPollution
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// --------------------------------------------
// DATASTORE EXTENSION
// --------------------------------------------
//private val Application.smrDataStore by preferencesDataStore("smr_ai_cache")

class SmrViewModel(app: Application) : AndroidViewModel(app) {
    init {
        Log.d("SmrViewModel", "SmrViewModel CREATED — instance=${System.identityHashCode(this)}")
    }
    // SMR LiveData
    private val _smr = MutableLiveData(Smr())
    val smr: LiveData<Smr> get() = _smr

    // AI Analysis
    private val _aiAnalysis = MutableLiveData<String>()
    val aiAnalysis: LiveData<String> get() = _aiAnalysis

    private val gson = Gson()
    // Track Module Progress
    private val _moduleProgress = MutableLiveData<Map<String,Int>>()
    val moduleProgress: LiveData<Map<String, Int>> get() = _moduleProgress
    private val _fileUrls = MutableLiveData<List<String>>(emptyList())
    val fileUrls: LiveData<List<String>> get() = _fileUrls
    private val SMR_DATA_KEY = stringPreferencesKey("smr_data")
    private val dataStore = (application as MyApplication).smrDataStore
    init{
        viewModelScope.launch {
            loadPersistedSmr()
        }
    }

    private suspend fun loadPersistedSmr() {
        val json = dataStore.data.first()[SMR_DATA_KEY]
        json?.let {
            try {
                val persistedSmr = gson.fromJson(it, Smr::class.java)
                _smr.value = persistedSmr
                updateProgress()
            } catch (_: Exception) {
                dataStore.edit { it.remove(SMR_DATA_KEY) }
            }
        }
    }

    private suspend fun saveSmrToDataStore(smr: Smr) {
        val json = gson.toJson(smr)
        dataStore.edit {preferences -> preferences[SMR_DATA_KEY] = json }
    }

//     ---------------------------------------------------------
//     DATASTORE KEYS
//     ---------------------------------------------------------
//    private val KEY_LAST_PROMPT_HASH = stringPreferencesKey("last_prompt_hash")
//    private val KEY_LAST_AI_OUTPUT = stringPreferencesKey("last_ai_output")

//     ---------------------------------------------------------
//     SAVE TO DATASTORE
//     ---------------------------------------------------------
//    private suspend fun saveToCache(promptHash: String, aiOutput: String) {
//        app.smrDataStore.edit { prefs ->
//            prefs[KEY_LAST_PROMPT_HASH] = promptHash
//            prefs[KEY_LAST_AI_OUTPUT] = aiOutput
//        }
//    }

    // ---------------------------------------------------------
    // LOAD FROM CACHE
    // ---------------------------------------------------------
//    private suspend fun loadCached(): Pair<String?, String?> {
//        val prefs = app.smrDataStore.data.first()
//        return Pair(
//            prefs[KEY_LAST_PROMPT_HASH],
//            prefs[KEY_LAST_AI_OUTPUT]
//        )
//    }

    // =========================================================
    // AI ANALYSIS WITH TIMEOUT + CACHE
    // =========================================================
//    fun analyzeSummary(prompt: String) {
//        viewModelScope.launch {

//            val promptHash = prompt.hashCode().toString()

//             LOAD CACHE FIRST
//            val (cachedHash, cachedOutput) = loadCached()

//             ---------------------------------------------
//             USE CACHE IF PROMPT DIDN’T CHANGE
//             ---------------------------------------------
//            if (cachedHash == promptHash && !cachedOutput.isNullOrEmpty()) {
//                _aiAnalysis.value = cachedOutput
//                return@launch
//            }

//             ---------------------------------------------
//             OTHERWISE → NEW AI REQUEST WITH TIMEOUT
//             ---------------------------------------------
//            try {
//                val request = ChatRequest(
//                    model = "gpt-4.1-mini",
//                    messages = listOf(
//                        ApiMessage(
//                            role = "system",
//                            content = "You are an environmental compliance AI assistant..."
//                        ),
//                        ApiMessage(
//                            role = "user",
//                            content = prompt
//                        )
//                    )
//                )
//
//                val response = withTimeout(10_000) {
//                    OpenAiClient.instance.getChatCompletion(request)
//                }
//
//                val output = response.choices.firstOrNull()?.message?.content
//                    ?: "No analysis generated."
//
//                _aiAnalysis.value = output
//                saveToCache(promptHash, output)

//            } catch (e: TimeoutCancellationException) {
//                if (!cachedOutput.isNullOrEmpty()) {
//                    _aiAnalysis.value =
//                        "⚠️ AI request timed out. Loaded cached analysis.\n\n$cachedOutput"
//                } else {
//                    _aiAnalysis.value =
//                        "⚠️ AI request timed out and no cached analysis is available."
//                }
//
//            } catch (e: Exception) {
//                _aiAnalysis.value = "AI analysis failed: ${e.message}"
//            }
//        }
//    }
//
    // =========================================================
    // SMR MODULE UPDATES
    // =========================================================

    fun updateGeneralInfo(generalInfo: GeneralInfo) {
        val currentSmr = _smr.value ?: Smr()
        val updatedSmr = currentSmr.copy(generalInfo = generalInfo)
        _smr.value = updatedSmr
        updateProgress()
        viewModelScope.launch{saveSmrToDataStore(updatedSmr)}
    }

    // ---------------- MODULE 2 ----------------
    fun updateHazardousWastes(hazardousWastes: List<HazardousWaste>) {
        val currentSmr = _smr.value ?: Smr()
        val updatedSmr = currentSmr.copy(hazardousWastes = hazardousWastes)
        _smr.value = updatedSmr
        updateProgress()
        viewModelScope.launch{saveSmrToDataStore(updatedSmr)}
    }

    // ---------------- MODULE 3 ----------------

    fun updateWaterPollutionRecords(waterPollutionRecords: List<WaterPollution>) {
        val currentSmr = _smr.value ?: Smr()
        val updatedSmr = currentSmr.copy(waterPollutionRecords = waterPollutionRecords)
        _smr.value = updatedSmr
        updateProgress()
        viewModelScope.launch{saveSmrToDataStore(updatedSmr)}
    }

    fun clearWaterPollutionRecords() {
        val current = _smr.value ?: Smr()
        _smr.value = current.copy(waterPollutionRecords = emptyList())
        updateModuleProgress("module3", 0)
    }

    // ---------------- MODULE 4 ----------------
    fun updateAirPollution(airPollution: AirPollution) {
        val currentSmr = _smr.value ?: Smr()
        val updatedSmr = currentSmr.copy(airPollution = airPollution)
        _smr.value = updatedSmr
        updateProgress()
        viewModelScope.launch{saveSmrToDataStore(updatedSmr)}
    }

    // ---------------- MODULE 5 ----------------
    fun updateOthers(others: Others) {
        val currentSmr = _smr.value ?: Smr()
        val updatedSmr = currentSmr.copy(others = others)
        _smr.value = updatedSmr
        updateProgress()
        viewModelScope.launch{saveSmrToDataStore(updatedSmr)}
    }

    fun updateProgress(){
        val currentSmr = _smr.value ?: return
        val progress = mapOf(
            "module1" to calculateGeneralInfoPercentage(currentSmr.generalInfo),
            "module2" to calculateListModulePercentage(currentSmr.hazardousWastes),
            "module3" to calculateListModulePercentage(currentSmr.waterPollutionRecords),
            "module4" to calculateAirPollutionProgress(currentSmr.airPollution),
            "module5" to calculateOthersProgress(currentSmr.others)
        )
        _moduleProgress.value = progress
    }

    // ---------------- CLEAR ALL ----------------
    fun clearSmr() {
        _smr.value = Smr()
        _moduleProgress.value = mapOf()
        viewModelScope.launch{
            dataStore.edit { it.remove(SMR_DATA_KEY) }
        }
    }

    // =========================================================
    // UTILITY FUNCTIONS
    // =========================================================

    private fun updateModuleProgress(key: String, percent: Int) {
        val map = _moduleProgress.value!!.toMutableMap()
        map[key] = percent.coerceIn(0, 100)
        _moduleProgress.value = map
    }

    private fun calculateGeneralInfoPercentage(generalInfo: GeneralInfo): Int {
        val fields = listOf(
            generalInfo.establishmentName,
            generalInfo.address,
            generalInfo.ownerName,
            generalInfo.phone,
            generalInfo.email,
            generalInfo.typeOfBusiness,
            generalInfo.ceoName,
            generalInfo.ceoPhone,
            generalInfo.ceoEmail,
            generalInfo.pcoName,
            generalInfo.pcoPhone,
            generalInfo.pcoEmail,
            generalInfo.pcoAccreditationNo,
            generalInfo.legalClassification
        )
        val filled = fields.count { it!!.isNotEmpty() }
        return (filled.toFloat() / fields.size * 100).toInt()
    }

    private fun calculateListModulePercentage(list: List<*>): Int =
        ((list.size.coerceAtMost(5) / 5f) * 100).toInt()

    private fun calculateAirPollutionProgress(airPollution: AirPollution): Int {
        val fields = listOf(
            airPollution.processEquipment,
            airPollution.location,
            airPollution.hoursOperation,
            airPollution.fuelEquipment,
            airPollution.fuelUsed,
            airPollution.fuelQuantity,
            airPollution.fuelHours,
            airPollution.pcfName,
            airPollution.pcfLocation,
            airPollution.pcfHours,
            airPollution.totalElectricity,
            airPollution.overheadCost,
            airPollution.emissionDescription,
            airPollution.emissionDate,
            airPollution.flowRate,
            airPollution.co,
            airPollution.nox,
            airPollution.particulates
        )
        val filled = fields.count { it.isNotEmpty() }
        return (filled.toFloat() / fields.size * 100).toInt()
    }

    private fun calculateOthersProgress(others: Others): Int {
        val fields = listOf(
            others.accidentDate,
            others.accidentArea,
            others.findings,
            others.actionsTaken,
            others.remarks,
            others.trainingDate,
            others.trainingDescription,
            others.personnelTrained
        )
        val filled = fields.count { it.isNotEmpty() }
        return (filled.toFloat() / fields.size * 100).toInt()
    }

    fun addFileUrl(url:String){
        val current = _fileUrls.value ?: emptyList()
        _fileUrls.value = current + url
        updateSmrWithFiles()
    }

    fun removeFileUrl(url: String) {
        val current = _fileUrls.value ?: emptyList()
        _fileUrls.value = current - url
        updateSmrWithFiles()
    }

    private fun updateSmrWithFiles() {
        val currentSmr = _smr.value ?: Smr()
        _smr.value = currentSmr.copy(fileUrls = _fileUrls.value ?: emptyList())
    }

    fun clearFiles() {
        _fileUrls.value = emptyList()
    }
}
