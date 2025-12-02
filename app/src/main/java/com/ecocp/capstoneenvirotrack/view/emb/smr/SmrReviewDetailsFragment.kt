package com.ecocp.capstoneenvirotrack.view.emb.smr

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.R
import androidx.navigation.fragment.findNavController
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
import com.ecocp.capstoneenvirotrack.utils.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("UNCHECKED_CAST")
class SmrReviewDetailsFragment: Fragment() {
    private var _binding: FragmentSmrReviewDetailsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var submissionId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
       _binding =  FragmentSmrReviewDetailsBinding.inflate(inflater, container, false)
        submissionId = arguments?.getString("submissionId")
        return binding.root
    }

    override fun onViewCreated( view:View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(submissionId != null){
            fetchSmrDetails(submissionId!!)
        } else{
            Snackbar.make(binding.root, "No submissions selected", Snackbar.LENGTH_SHORT).show()
        }

        binding.btnBack.setOnClickListener{
            findNavController().navigate(R.id.action_embSmrReviewDetailsFragment_to_embSmrDashboardFragment)
        }
    }

    private fun fetchSmrDetails(submissionId:String){
        db.collection("smr_submissions").document(submissionId)
            .get()
            .addOnSuccessListener { doc ->
                if(!doc.exists()) {
                    Snackbar.make(binding.root, "No submissions selected", Snackbar.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val generalInfoMap = doc.get("generalInfo") as? Map<*, *>
                val generalInfo = generalInfoMap?.let{
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

                val waterPollutionRecords = (doc.get("waterPollutionRecords") as? List<Map<String, Any>>)?.map {
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

                displaySmrData(smr)
                performAiAnalysis(smr)
            }.addOnFailureListener {
                Snackbar.make(binding.root, "Error loading SMR details", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun displaySmrData(smr: Smr){
        binding.tvGeneralInfo.text = smr.generalInfo.generalInfoText()
        binding.tvHazardousWastes.text = smr.hazardousWastes.hazardousWasteText()
        binding.tvWaterPollution.text = smr.waterPollutionRecords.waterPollutionText()
        binding.tvAirPollution.text = smr.airPollution.airPollutionText()
        binding.tvOthers.text = smr.others.othersText()
    }

    @SuppressLint("SetTextI18n")
    private fun performAiAnalysis(smr: Smr) {
        val prompt = """
            You are an Environmental Compliance Analyst for the EMB (Environmental Management Bureau).
            Review this SMR submission for compliance with environmental regulations.
            Provide a structured analysis including:
            1. Overall Compliance Assessment
            2. Identified Issues or Violations
            3. Recommendations for Improvement
            4. Suggested Follow-up Actions
            
            SMR Data:
            ${buildFullSummary(smr)}
        """.trimIndent()

        CoroutineScope(Dispatchers.IO).launch{
            try{
                val request = OpenAiRequest(
                    model = "gpt-4.1-mini",
                    messages = listOf(OpenAiMessage(role = "user", content = prompt)),
                    max_tokens = 1000
                )
                val response = OpenAiClient.instance.getChatCompletion(request)
                val analysis = response.choices.firstOrNull()?.message?.content ?: "No analysis generated."

                withContext(Dispatchers.Main){
                    binding.tvAiAnalysis.text = analysis
                }
            }catch(e:Exception){
                Log.e("AI Analysis", "Error: ${e.message}")
                withContext(Dispatchers.Main){
                    binding.tvAiAnalysis.text = "AI analysis failed: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun buildFullSummary(smr:Smr):String{
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

    override fun onDestroyView(){
        super.onDestroyView()
        _binding = null
    }
}