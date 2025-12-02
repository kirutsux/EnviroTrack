package com.ecocp.capstoneenvirotrack.view.businesses.smr

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentSmrSummaryBinding
import com.ecocp.capstoneenvirotrack.model.*
import com.ecocp.capstoneenvirotrack.viewmodel.SmrViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ecocp.capstoneenvirotrack.utils.*

class SmrSummaryFragment : Fragment() {

    private var _binding: FragmentSmrSummaryBinding? = null
    private val binding get() = _binding!!
    private val smrViewModel: SmrViewModel by activityViewModels()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSmrSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe the smr LiveData so summary updates automatically when modules change
        smrViewModel.smr.observe(viewLifecycleOwner) { smr ->
            // update UI from the latest smr
            displaySmrData(smr)

            // rebuild summary and run AI analysis (optional: move analyzeSummary call to a button if you
            // want to avoid running analysis on every small change)
            val smrSummary = buildFullSmrSummary(smr)

            val aiPrompt = """
                ROLE:
                You are an Environmental Compliance Analyst for the Environmental Management Bureau (EMB).
                Your task is to evaluate Self-Monitoring Reports (SMR) for Modules 1–5.
                Provide strictly evidence-based insights ONLY from the supplied SMR summary.
                Do NOT fabricate missing information, values, or environmental limits.

                INPUT:
                Here is the complete SMR submitted by the facility:
                --- BEGIN SMR DATA ---
                $smrSummary
                --- END SMR DATA ---

                IMPORTANT RULES:
                - Each module (1 to 5) MUST include an **Initial Findings** section.
                - Each Initial Finding must be **1–2 sentences** summarizing ONLY the most important SMR data.
                - Use ONLY the data available in the SMR summary.
                - No assumptions or invented values.
                - Output must remain consistent when the same data is provided (supports caching).

                STEPS:
                1. Read the entire SMR.
                2. Extract only factual entries.
                3. Identify important values per module and convert them into sentence-form initial findings.
                4. Identify missing data, contradictions, unusual patterns, or suspicious values.
                5. Only flag compliance concerns when the SMR itself provides supporting evidence.

                EXPECTED OUTPUT FORMAT:

                **MODULE 1 — Initial Findings**
                - 1–2 sentences summarizing Module 1 data.

                **MODULE 2 — Initial Findings**
                - 1–2 sentences summarizing Module 2 data.

                **MODULE 3 — Initial Findings**
                - 1–2 sentences summarizing Module 3 data.

                **MODULE 4 — Initial Findings**
                - 1–2 sentences summarizing Module 4 data.

                **MODULE 5 — Initial Findings**
                - 1–2 sentences summarizing Module 5 data.

                ---
                **Overall Automated Analysis & Observations**
                **Potential Compliance Concerns**
                **Recommended Actions for EMB Review**
                **Confidence Level**
            """.trimIndent()

//            smrViewModel.analyzeSummary(aiPrompt)
        }

        binding.btnSubmitSmr.setOnClickListener {
            submitSmrToFirebase()
        }

        observeAiAnalysis()
    }

    /** --- DISPLAY SUMMARY DATA --- **/
    @SuppressLint("SetTextI18n")
    private fun displaySmrData(smr: Smr) {
        // Module 1: General Info
        val module1Text = smr.generalInfo.generalInfoText()
        binding.module1Container.tvModuleTitle.text = "Module 1: General Information"
        binding.module1Container.tvModuleSummary.text = module1Text
        android.util.Log.d("SMRDisplay", "Module 1: $module1Text")

        // Module 2: Hazardous Waste
        val module2Text = smr.hazardousWastes.hazardousWasteText()
        binding.module2Container.tvModuleTitle.text = "Module 2: Hazardous Waste"
        binding.module2Container.tvModuleSummary.text = module2Text
        android.util.Log.d("SMRDisplay", "Module 2: $module2Text")

        // Module 3: Water Pollution
        val module3Text = smr.waterPollutionRecords.waterPollutionText()
        binding.module3Container.tvModuleTitle.text = "Module 3: Water Pollution"
        binding.module3Container.tvModuleSummary.text = module3Text
        android.util.Log.d("SMRDisplay", "Module 3: $module3Text")

        // Module 4: Air Pollution
        val module4Text = smr.airPollution.airPollutionText()
        binding.module4Container.tvModuleTitle.text = "Module 4: Air Pollution"
        binding.module4Container.tvModuleSummary.text = module4Text
        android.util.Log.d("SMRDisplay", "Module 4: $module4Text")

        // Module 5: Others
        val module5Text = smr.others.othersText()
        binding.module5Container.tvModuleTitle.text = "Module 5: Others"
        binding.module5Container.tvModuleSummary.text = module5Text
        android.util.Log.d("SMRDisplay", "Module 5: $module5Text")
    }

    private fun buildFullSmrSummary(smr: Smr): String {
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

    private fun observeAiAnalysis() {
        smrViewModel.aiAnalysis.observe(viewLifecycleOwner) { analysis ->
            binding.module6Container.tvAiAnalysis.text =
                analysis ?: "No AI analysis provided."
        }
    }

    /** --- SAVE TO FIREBASE --- **/
    private fun submitSmrToFirebase() {
        val smr = smrViewModel.smr.value
        val userUid = FirebaseAuth.getInstance().currentUser?.uid

        if (smr == null || userUid == null) {
            Snackbar.make(binding.root, "No SMR data or user UID found.", Snackbar.LENGTH_SHORT).show()
            return
        }

        val smrData = mapOf(
            "dateSubmitted" to Timestamp.now(),
            "uid" to userUid,
            "generalInfo" to smr.generalInfo,
            "hazardousWastes" to smr.hazardousWastes,
            "waterPollutionRecords" to smr.waterPollutionRecords,
            "airPollution" to smr.airPollution,
            "others" to smr.others
        )

        firestore.collection("smr_submissions")
            .add(smrData)
            .addOnSuccessListener {
                Snackbar.make(binding.root, "SMR successfully submitted!", Snackbar.LENGTH_SHORT).show()
                smrViewModel.clearSmr()
                clearAllInputs() // clear all input fields

                // Navigate to SmrDashboardFragment
                findNavController().navigate(R.id.action_smrSummaryFragment_to_smrDashboardFragment)
            }
            .addOnFailureListener { e ->
                Snackbar.make(binding.root, "Failed to submit SMR: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
    }

    /** --- CLEAR ALL MODULE INPUT FIELDS --- **/
    private fun clearAllInputs() {
        smrViewModel.updateGeneralInfo(GeneralInfo())
        smrViewModel.updateHazardousWastes(emptyList())
        smrViewModel.clearWaterPollutionRecords()
        smrViewModel.updateAirPollution(AirPollution())
        smrViewModel.updateOthers(Others())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
