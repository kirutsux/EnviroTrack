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

        displaySmrData()

        val fullSummary = buildFullSmrSummary()
        smrViewModel.analyzeSummary(fullSummary)

        binding.btnSubmitSmr.setOnClickListener {
            submitSmrToFirebase()
        }

        smrViewModel.aiAnalysis.observe(viewLifecycleOwner) { analysis ->
            binding.module6Container.tvAiAnalysis.text = analysis?: "No AI analysis provided."
        }

        observeAiAnalysis()
    }

    /** --- DISPLAY SUMMARY DATA --- **/
    @SuppressLint("SetTextI18n")
    private fun displaySmrData() {
        val smr = smrViewModel.smr.value

        // Module 1: General Info
        binding.module1Container.tvModuleTitle.text = "Module 1: General Information"
        binding.module1Container.tvModuleSummary.text =
            smr?.generalInfo?.generalInfoText() ?: "No data provided"

        // Module 2: Water Pollution
        binding.module2Container.tvModuleTitle.text = "Module 2: Water Pollution"
        binding.module2Container.tvModuleSummary.text =
            smr?.waterPollutionRecords?.waterPollutionText() ?: "No data provided"

        // Module 3: Air Pollution
        binding.module3Container.tvModuleTitle.text = "Module 3: Air Pollution"
        binding.module3Container.tvModuleSummary.text =
            smr?.airPollution?.airPollutionText() ?: "No data provided"

        // Module 4: Hazardous Waste
        binding.module4Container.tvModuleTitle.text = "Module 4: Hazardous Waste"
        binding.module4Container.tvModuleSummary.text =
            smr?.hazardousWastes?.hazardousWasteText() ?: "No data provided"

        // Module 5: Others
        binding.module5Container.tvModuleTitle.text = "Module 5: Others"
        binding.module5Container.tvModuleSummary.text =
            smr?.others?.othersText() ?: "No data provided"
    }

    private fun buildFullSmrSummary(): String {
        val smr = smrViewModel.smr.value ?: return "No SMR data available."

        return """
            SELF-MONITORING REPORT SUMMARY
            --- MODULE 1: GENERAL INFORMATION ---
            ${smr.generalInfo.generalInfoText()}
            
            --- MODULE 2: WATER POLLUTION ---
            ${smr.waterPollutionRecords.waterPollutionText()}
            
            --- MODULE 3: AIR POLLUTION ---
            ${smr.airPollution.airPollutionText()}
            
            --- MODULE 4: HAZARDOUS WASTE ---
            ${smr.hazardousWastes.hazardousWasteText()}
            
            --- MODULE 5: OTHERS ---
            ${smr.others.othersText()}
            """.trimIndent()
    }

    private fun observeAiAnalysis(){
        smrViewModel.aiAnalysis.observe(viewLifecycleOwner) { analysis ->
            binding.module6Container.tvAiAnalysis.text =
                analysis ?: "No AI analysis provided."
        }
    }

    // Extension functions for generating module text
    private fun GeneralInfo.generalInfoText() = """
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

    private fun List<WaterPollution>.waterPollutionText() = mapIndexed { i, wp ->
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

    private fun AirPollution.airPollutionText() = """
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

    private fun List<HazardousWaste>.hazardousWasteText() = mapIndexed { i, hw ->
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

    private fun Others.othersText() = """
        Accident Date: $accidentDate
        Accident Area: $accidentArea
        Findings: $findings
        Actions Taken: $actionsTaken
        Remarks: $remarks
        Training Date: $trainingDate
        Training Description: $trainingDescription
        Personnel Trained: $personnelTrained
    """.trimIndent()

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
