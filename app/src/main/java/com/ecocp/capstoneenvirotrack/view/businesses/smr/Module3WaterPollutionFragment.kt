package com.ecocp.capstoneenvirotrack.view.businesses.smr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentModule3WaterPollutionBinding
import com.ecocp.capstoneenvirotrack.model.WaterPollution
import com.ecocp.capstoneenvirotrack.viewmodel.SmrViewModel
import com.ecocp.capstoneenvirotrack.viewmodel.SmrViewModelFactory

class Module3WaterPollutionFragment : Fragment() {

    private var _binding: FragmentModule3WaterPollutionBinding? = null
    private val binding get() = _binding!!

    private val smrViewModel: SmrViewModel by activityViewModels {
        SmrViewModelFactory(requireActivity().application)
    }
    private val waterPollutionList = mutableListOf<WaterPollution>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModule3WaterPollutionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load existing records from ViewModel if any
        smrViewModel.smr.value?.waterPollutionRecords?.let { waterPollutionList.addAll(it) }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // Add a new water pollution record
        binding.AddWaterPollution.setOnClickListener {
            val record = collectWaterPollutionInput()
            record?.let {
                if (!waterPollutionList.contains(it)) {
                    waterPollutionList.add(it)
                    smrViewModel.updateWaterPollutionRecords(waterPollutionList)
                    Toast.makeText(requireContext(), "Water pollution record added!", Toast.LENGTH_SHORT).show()
                    clearFields()
                } else {
                    Toast.makeText(requireContext(), "This record already exists.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Save module partially
        binding.SaveModule.setOnClickListener {
            smrViewModel.updateWaterPollutionRecords(waterPollutionList)
            Toast.makeText(requireContext(), "Module saved successfully!", Toast.LENGTH_SHORT).show()
        }

        // Navigate to Module 4
        binding.btnNextModule4.setOnClickListener {
            smrViewModel.updateWaterPollutionRecords(waterPollutionList)
            findNavController().navigate(R.id.action_module3WaterPollutionFragment_to_module4AirPollutionFragment)
        }
    }

    /** Collect input fields safely into WaterPollution object */
    private fun collectWaterPollutionInput(): WaterPollution? {
        val domesticWastewater = binding.inputDomesticWastewater.text.toString().toDoubleOrNull() ?: 0.0
        val processWastewater = binding.inputProcessWastewater.text.toString().toDoubleOrNull() ?: 0.0

        if (domesticWastewater == 0.0 && processWastewater == 0.0) {
            Toast.makeText(requireContext(), "Please enter at least one wastewater value.", Toast.LENGTH_SHORT).show()
            return null
        }

        return WaterPollution(
            domesticWastewater = domesticWastewater,
            processWastewater = processWastewater,
            coolingWater = binding.inputCoolingWater.text.toString(),
            otherSource = binding.inputOtherSource.text.toString(),
            washEquipment = binding.inputWashEquipment.text.toString(),
            washFloor = binding.inputWashFloor.text.toString(),
            employees = binding.inputEmployees.text.toString().toIntOrNull() ?: 0,
            costEmployees = binding.inputCostEmployees.text.toString(),
            utilityCost = binding.inputUtilityCost.text.toString(),
            newInvestmentCost = binding.inputNewInvestmentCost.text.toString(),
            outletNo = binding.inputOutletNo.text.toString().toIntOrNull() ?: 0,
            outletLocation = binding.inputOutletLocation.text.toString(),
            waterBody = binding.inputWaterBody.text.toString(),
            date1 = binding.inputDate1.text.toString(),
            flow1 = binding.inputFlow1.text.toString(),
            bod1 = binding.inputBod1.text.toString(),
            tss1 = binding.inputTss1.text.toString(),
            color1 = binding.inputColor1.text.toString(),
            ph1 = binding.inputPh1.text.toString(),
            oilGrease1 = binding.inputOilGrease1.text.toString(),
            tempRise1 = binding.inputTempRise1.text.toString(),
            do1 = binding.inputDo1.text.toString()
        )
    }

    /** Clear input fields after adding a record */
    private fun clearFields() = with(binding) {
        inputDomesticWastewater.text?.clear()
        inputProcessWastewater.text?.clear()
        inputCoolingWater.text?.clear()
        inputOtherSource.text?.clear()
        inputWashEquipment.text?.clear()
        inputWashFloor.text?.clear()
        inputEmployees.text?.clear()
        inputCostEmployees.text?.clear()
        inputUtilityCost.text?.clear()
        inputNewInvestmentCost.text?.clear()
        inputOutletNo.text?.clear()
        inputOutletLocation.text?.clear()
        inputWaterBody.text?.clear()
        inputDate1.text?.clear()
        inputFlow1.text?.clear()
        inputBod1.text?.clear()
        inputTss1.text?.clear()
        inputColor1.text?.clear()
        inputPh1.text?.clear()
        inputOilGrease1.text?.clear()
        inputTempRise1.text?.clear()
        inputDo1.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
