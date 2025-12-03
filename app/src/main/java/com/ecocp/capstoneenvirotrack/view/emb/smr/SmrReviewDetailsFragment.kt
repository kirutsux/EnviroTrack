package com.ecocp.capstoneenvirotrack.view.emb.smr

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.MyApplication
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.ModuleAdapter
import com.ecocp.capstoneenvirotrack.api.OpenAiClient
import com.ecocp.capstoneenvirotrack.databinding.FragmentSmrReviewDetailsBinding
import com.ecocp.capstoneenvirotrack.model.AirPollution
import com.ecocp.capstoneenvirotrack.model.GeneralInfo
import com.ecocp.capstoneenvirotrack.model.HazardousWaste
import com.ecocp.capstoneenvirotrack.model.OpenAiMessage
import com.ecocp.capstoneenvirotrack.model.OpenAiRequest
import com.ecocp.capstoneenvirotrack.model.Others
import com.ecocp.capstoneenvirotrack.model.Smr
import com.ecocp.capstoneenvirotrack.model.WaterPollution
import com.ecocp.capstoneenvirotrack.utils.airPollutionText
import com.ecocp.capstoneenvirotrack.utils.generalInfoText
import com.ecocp.capstoneenvirotrack.utils.hazardousWasteText
import com.ecocp.capstoneenvirotrack.utils.othersText
import com.ecocp.capstoneenvirotrack.utils.waterPollutionText
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@Suppress("UNCHECKED_CAST", "PrivatePropertyName")
class SmrReviewDetailsFragment : Fragment() {
    private var _binding: FragmentSmrReviewDetailsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var submissionId: String? = null
    private lateinit var smr: Smr


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSmrReviewDetailsBinding.inflate(inflater, container, false)
        submissionId = arguments?.getString("submissionId")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAnalyze.isEnabled = false
        binding.tvAiAnalysis.visibility = View.GONE

        if (submissionId != null) {
            fetchSmrDetails(submissionId!!)
        } else {
            Snackbar.make(binding.root, "No submissions selected", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun fetchSmrDetails(submissionId: String) {
        db.collection("smr_submissions").document(submissionId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Snackbar.make(binding.root, "No submissions selected", Snackbar.LENGTH_SHORT)
                        .show()
                    return@addOnSuccessListener
                }

                val generalInfoMap = doc.get("generalInfo") as? Map<*, *>
                val generalInfo = generalInfoMap?.let {
                    GeneralInfo(
                        establishmentName = it["establishmentName"] as? String ?: "",
                        address = it["address"] as? String ?: "",
                        ownerName = it["ownerName"] as? String ?: "",
                        phone = it["phone"] as? String ?: "",
                        email = it["email"] as? String ?: "",
                        typeOfBusiness = it["typeOfBusiness"] as? String ?: "",
                        ceoName = it["ceoName"] as? String,
                        ceoPhone = it["ceoPhone"] as? String,
                        ceoEmail = it["ceoEmail"] as? String,
                        pcoName = it["pcoName"] as? String ?: "",
                        pcoPhone = it["pcoPhone"] as? String ?: "",
                        pcoEmail = it["pcoEmail"] as? String ?: "",
                        pcoAccreditationNo = it["pcoAccreditationNo"] as? String ?: "",
                        legalClassification = it["legalClassification"] as? String ?: ""
                    )
                } ?: GeneralInfo()

                val hazardousWastesList = doc.get("hazardousWastes") as? List<Map<*, *>>
                val hazardousWastes = hazardousWastesList?.map {
                    HazardousWaste(
                        commonName = it["commonName"] as? String ?: "",
                        casNo = it["casNo"] as? String ?: "",
                        tradeName = it["tradeName"] as? String ?: "",
                        hwNo = it["hwNo"] as? String ?: "",
                        hwClass = it["hwClass"] as? String ?: "",
                        hwGenerated = it["hwGenerated"] as? String ?: "",
                        storageMethod = it["storageMethod"] as? String ?: "",
                        transporter = it["transporter"] as? String ?: "",
                        treater = it["treater"] as? String ?: "",
                        disposalMethod = it["disposalMethod"] as? String ?: ""
                    )
                } ?: emptyList()

                val waterPollutionRecords =
                    (doc.get("waterPollutionRecords") as? List<Map<String, Any>>)?.map {
                        WaterPollution(
                            domesticWastewater = (it["domesticWastewater"] as? Double) ?: 0.0,
                            processWastewater = (it["processWastewater"] as? Double) ?: 0.0,
                            // Map other fields as needed
                            date1 = it["date1"] as? String ?: "",
                            flow1 = it["flow1"] as? String ?: "",
                            bod1 = it["bod1"] as? String ?: "",
                            tss1 = it["tss1"] as? String ?: "",
                            color1 = it["color1"] as? String ?: "",
                            ph1 = it["ph1"] as? String ?: "",
                            oilGrease1 = it["oilGrease1"] as? String ?: "",
                            tempRise1 = it["tempRise1"] as? String ?: "",
                            do1 = it["do1"] as? String ?: ""
                        )
                    } ?: emptyList()

                val airPollution = (doc.get("airPollution") as? Map<String, Any>)?.let {
                    AirPollution(
                        processEquipment = it["processEquipment"] as? String ?: "",
                        location = it["location"] as? String ?: "",
                        hoursOperation = it["hoursOperation"] as? String ?: "",
                        fuelEquipment = it["fuelEquipment"] as? String ?: "",
                        fuelUsed = it["fuelUsed"] as? String ?: "",
                        fuelQuantity = it["fuelQuantity"] as? String ?: "",
                        fuelHours = it["fuelHours"] as? String ?: "",
                        pcfName = it["pcfName"] as? String ?: "",
                        pcfLocation = it["pcfLocation"] as? String ?: "",
                        pcfHours = it["pcfHours"] as? String ?: "",
                        totalElectricity = it["totalElectricity"] as? String ?: "",
                        overheadCost = it["overheadCost"] as? String ?: "",
                        emissionDescription = it["emissionDescription"] as? String ?: "",
                        emissionDate = it["emissionDate"] as? String ?: "",
                        flowRate = it["flowRate"] as? String ?: "",
                        co = it["co"] as? String ?: "",
                        nox = it["nox"] as? String ?: "",
                        particulates = it["particulates"] as? String ?: ""
                    )
                } ?: AirPollution()

                val others = (doc.get("others") as? Map<String, Any>)?.let {
                    Others(
                        accidentDate = it["accidentDate"] as? String ?: "",
                        accidentArea = it["accidentArea"] as? String ?: "",
                        findings = it["findings"] as? String ?: "",
                        actionsTaken = it["actionsTaken"] as? String ?: "",
                        remarks = it["remarks"] as? String ?: "",
                        trainingDate = it["trainingDate"] as? String ?: "",
                        trainingDescription = it["trainingDescription"] as? String ?: "",
                        personnelTrained = it["personnelTrained"] as? String ?: ""
                    )
                } ?: Others()

                val smr = Smr(
                    generalInfo = generalInfo,
                    hazardousWastes = hazardousWastes,
                    waterPollutionRecords = waterPollutionRecords,
                    airPollution = airPollution,
                    others = others,
                    submittedAt = (doc.getTimestamp("dateSubmitted")?.toDate()?.time) ?: 0L,
                    uid = doc.getString("uid"),
                    id = doc.id
                )

                this.smr = smr
                displaySummary(smr)

                binding.btnAnalyze.isEnabled = true
                setupButtons()
            }.addOnFailureListener {
                Snackbar.make(binding.root, "Error loading SMR details", Snackbar.LENGTH_SHORT)
                    .show()
            }

    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_embSmrReviewDetailsFragment_to_embSmrDashboardFragment)
        }
        binding.btnAnalyze.setOnClickListener {
            performAiAnalysis(smr)
        }
        binding.btnApprove.setOnClickListener {
            updateSmrStatus("Approved", null)
        }

        binding.btnReject.setOnClickListener {
            showRejectionDialog()
        }
    }

    private fun updateSmrStatus(newStatus: String, rejectionReason: String?) {
        val updates = mutableMapOf<String, Any>("status" to newStatus)
        rejectionReason?.let{ updates["rejectionReason"] = it }

        db.collection("smr_submissions").document(smr.id!!)
            .update(updates)
            .addOnSuccessListener {
                Snackbar.make(binding.root, "SMR status updated to $newStatus", Snackbar.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_embSmrReviewDetailsFragment_to_embSmrDashboardFragment)
            }
            .addOnFailureListener{e->
                Snackbar.make(binding.root, "Failed to update status: ${e.message}", Snackbar.LENGTH_SHORT).show()
                Log.e("StatusUpdate", "Failed to update status: ${e.message}")
            }
    }

    private fun showRejectionDialog(){

        val input = android.widget.EditText(requireContext()).apply {
            hint = "Enter rejection reason"
            setSingleLine(false)
            maxLines = 5
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Reject SMR")
            .setMessage("Provide a reason for rejection:")
            .setView(input)
            .setPositiveButton("Reject") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isNotEmpty()) {
                    updateSmrStatus("Rejected", reason)
                } else {
                    Snackbar.make(binding.root, "Rejection reason is required", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun displaySummary(smr: Smr) {
        val totalModules = 5
        var completedModules = 0
        if (smr.generalInfo.establishmentName.isNotEmpty()) completedModules++
        if (smr.hazardousWastes.isNotEmpty()) completedModules++
        if (smr.waterPollutionRecords.isNotEmpty()) completedModules++
        if (smr.airPollution.processEquipment.isNotEmpty()) completedModules++
        if (smr.others.accidentDate.isNotEmpty()) completedModules++
        val percentage = (completedModules.toFloat() / totalModules * 100).toInt()

        val modules = listOf(
            "General Information" to smr.generalInfo.generalInfoText(),
            "Hazardous Wastes" to smr.hazardousWastes.hazardousWasteText(),
            "Water Pollution" to smr.waterPollutionRecords.waterPollutionText(),
            "Air Pollution" to smr.airPollution.airPollutionText(),
            "Others" to smr.others.othersText()
        )

        val adapter = ModuleAdapter(modules) { moduleName, details ->
            showModuleDetailsDialog(moduleName, details)
        }
        binding.recyclerModules.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerModules.adapter = adapter
    }

    private fun showModuleDetailsDialog(moduleName: String, details: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(moduleName)
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    @Suppress("PrivatePropertyName")
    private val KEY_LAST_PROMPT_HASH = stringPreferencesKey("last_prompt_hash")
    private val KEY_LAST_AI_OUTPUT = stringPreferencesKey("last_ai_output")

    @SuppressLint("SetTextI18n")
    private fun performAiAnalysis(smr: Smr) {
        val modules = listOf(
            "General Information" to smr.generalInfo.generalInfoText(),
            "Hazardous Wastes" to smr.hazardousWastes.hazardousWasteText(),
            "Water Pollution" to smr.waterPollutionRecords.waterPollutionText(),
            "Air Pollution" to smr.airPollution.airPollutionText(),
            "Others" to smr.others.othersText()
        )

        val analyses = mutableListOf<String>()
        var currentModuleIndex = 0

        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("Analyzing...")
            .setMessage("Please wait while we analyze the SMR.")
            .setCancelable(false)
            .create()
        val progressBar = ProgressBar(requireContext()).apply {
            isIndeterminate = true
        }
        progressDialog.setView(progressBar)
        progressDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                for ((moduleName, moduleData) in modules) {
                    withContext(Dispatchers.Main) {
                        if (_binding != null) {
                            progressDialog.setMessage("Analyzing $moduleName...")
                        }
                    }

                    val prompt = """
                        You are an Environmental Compliance Analyst for the EMB (Environmental Management Bureau).
                        Here are a list of different modules from an accredited PCO for their quarterly requirements.
                        Provide a short and structured compliance assessment of each:
                        Module Name:${moduleName}
                        Module Data:${moduleData}
                    """.trimIndent()

                    val request = OpenAiRequest(
                        model = "gpt-3.5-turbo",
                        messages = listOf(OpenAiMessage(role = "user", content = prompt)),
                        max_tokens = 500
                    )

                    val response = withTimeout(3_600_000) {
                        OpenAiClient.instance.getChatCompletion(request)
                    }

                    val moduleAnalysis = response.choices.firstOrNull()?.message?.content
                        ?: "No analysis for $moduleName"

                    analyses.add("$moduleName: $moduleAnalysis")
                    currentModuleIndex++

                    withContext(Dispatchers.Main) {
                        progressDialog.setMessage("Completed $moduleName. Processing next...")
                    }
                }

                val fullAnalysis = analyses.joinToString("\n\n") { it }
                val finalPrompt = """
                    Compile the following module analyses into a comprehensive SMR compliance report:
                    $fullAnalysis
                    Provide an overall assessment with structured criticism. Provide an assessment on their issues, recommendations, and follow-up steps.
                """.trimIndent()

                val finalRequest = OpenAiRequest(
                    model = "gpt-3.5-turbo",
                    messages = listOf(OpenAiMessage(role = "user", content = finalPrompt)),
                    max_tokens = 1500
                )

                withContext(Dispatchers.Main) {
                    progressDialog.setMessage("Finalizing analysis...")
                }

                val finalResponse = withTimeout(3_600_000) {
                    OpenAiClient.instance.getChatCompletion(finalRequest)
                }
                val compiledAnalysis =
                    finalResponse.choices.firstOrNull()?.message?.content ?: "Compilation failed."

                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        binding.tvAiAnalysis.text = compiledAnalysis
                        progressDialog.dismiss()

                        binding.tvCompletionPercentage.visibility = View.GONE
                        binding.recyclerModules.visibility = View.GONE
                        binding.btnAnalyze.visibility = View.GONE
                        binding.tvAiAnalysis.visibility = View.VISIBLE

                        db.collection("smr_submissions").document(smr.id!!)
                            .update("status", "Reviewed")
                            .addOnSuccessListener {
                                Log.d(
                                    "Status Update",
                                    "Status updated to Reviewed for ${smr.id}"
                                )
                            }
                            .addOnFailureListener { e ->
                                Log.d(
                                    "Status Update",
                                    "Status update failed. ${e.message}}"
                                )
                            }
                    }
                }

                saveToCache("full_analysis_${smr.id}", compiledAnalysis)
            } catch (_: TimeoutCancellationException) {
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        binding.tvAiAnalysis.text = "AI analysis timed out."
                        progressDialog.dismiss()
                    }
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        binding.tvAiAnalysis.text =
                            "AI analysis failed: HTTP ${e.code()} - $errorBody"
                        progressDialog.dismiss()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        binding.tvAiAnalysis.text = "AI analysis failed: ${e.localizedMessage}"
                        progressDialog.dismiss()
                    }
                }
            }
        }
    }

    @SuppressLint("UseKtx")
    private suspend fun saveToCache(promptHash: String, aiOutput: String) {
        val app = requireContext().applicationContext as? MyApplication
            ?: throw IllegalStateException("Application context is not MyApplication. Check AndroidManifest.xml and rebuild.")
        app.smrDataStore.edit { prefs ->
            prefs[KEY_LAST_PROMPT_HASH] = promptHash
            prefs[KEY_LAST_AI_OUTPUT] = aiOutput
        }
    }

    private suspend fun loadCached(): Pair<String?, String?> {
        val app = requireContext().applicationContext as? MyApplication
            ?: throw IllegalStateException("Application context is not MyApplication. Check AndroidManifest.xml and rebuild.")
        val prefs = app.smrDataStore.data.first()
        return Pair(
            prefs[KEY_LAST_PROMPT_HASH],
            prefs[KEY_LAST_AI_OUTPUT]
        )
    }


    private fun buildFullSummary(smr: Smr): String {
        return """
            SELF-MONITORING REPORT SUMMARY
            --- MODULE 1: GENERAL INFORMATION ---
            ${smr.generalInfo.generalInfoText()}
            --- MODULE 2: HAZARDOUS WASTE ---
            ${smr.hazardousWastes.hazardousWasteText()}
            --- MODULE 3: WATER POLLUTION ---
            ${smr.waterPollutionRecords.waterPollutionText()}
            --- MODULE 4: AIR POLLUTION ---
            ${smr.airPollution.airPollutionText()}
            --- MODULE 5: OTHERS ---
            ${smr.others.othersText()}
        """.trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}