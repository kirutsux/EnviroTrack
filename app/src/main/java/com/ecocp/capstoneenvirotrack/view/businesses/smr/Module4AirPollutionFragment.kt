package com.ecocp.capstoneenvirotrack.view.businesses.smr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentModule4AirPollutionBinding
import com.ecocp.capstoneenvirotrack.model.AirPollution
import com.ecocp.capstoneenvirotrack.viewmodel.SmrViewModel
import com.google.android.material.snackbar.Snackbar

class Module4AirPollutionFragment : Fragment() {

    private var _binding: FragmentModule4AirPollutionBinding? = null
    private val binding get() = _binding!!
    private val smrViewModel: SmrViewModel by activityViewModels()
    private var currentAirPollution: AirPollution? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModule4AirPollutionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Preload existing Air Pollution data if any
        smrViewModel.smr.value?.airPollution?.let {
            currentAirPollution = it
            populateFields(it)
        }

        /** --- Add / Save Air Pollution Entry --- */
        binding.btnAddAirPollution.setOnClickListener {
            val entry = collectAirPollutionInput() ?: return@setOnClickListener
            currentAirPollution = entry
            smrViewModel.updateAirPollution(entry)
            Snackbar.make(binding.root, "Air Pollution entry added!", Snackbar.LENGTH_SHORT).show()
            clearFields()
        }

        /** --- Save Module (partial save) --- */
        binding.SaveModule.setOnClickListener {
            saveAirPollutionData(partial = true)
        }

        /** --- Navigate to Module 5 --- */
        binding.btnNextModule5.setOnClickListener {
            saveAirPollutionData(partial = true)
            findNavController().navigate(R.id.action_module4AirPollutionFragment_to_module5OthersFragment)
        }
    }

    /** --- Collect input fields into an AirPollution object --- */
    private fun collectAirPollutionInput(): AirPollution? {
        if (binding.etEquipmentName.text.isNullOrBlank() &&
            binding.etLocation.text.isNullOrBlank()
        ) {
            Snackbar.make(binding.root, "Please enter at least one required field.", Snackbar.LENGTH_SHORT).show()
            return null
        }

        return AirPollution(
            processEquipment = binding.etEquipmentName.text.toString().trim(),
            location = binding.etLocation.text.toString().trim(),
            hoursOperation = binding.etHoursOperation.text.toString().trim(),
            fuelEquipment = binding.etFuelEquipment.text.toString().trim(),
            fuelUsed = binding.etFuelUsed.text.toString().trim(),
            fuelQuantity = binding.etFuelQuantity.text.toString().trim(),
            fuelHours = binding.etFuelHours.text.toString().trim(),
            pcfName = binding.etPcfName.text.toString().trim(),
            pcfLocation = binding.etPcfLocation.text.toString().trim(),
            pcfHours = binding.etPcfHours.text.toString().trim(),
            totalElectricity = binding.etTotalElectricity.text.toString().trim(),
            overheadCost = binding.etOverheadCost.text.toString().trim(),
            emissionDescription = binding.etEmissionDescription.text.toString().trim(),
            emissionDate = binding.etEmissionDate.text.toString().trim(),
            flowRate = binding.etFlowRate.text.toString().trim(),
            co = binding.etCO.text.toString().trim(),
            nox = binding.etNOx.text.toString().trim(),
            particulates = binding.etParticulates.text.toString().trim()
        )
    }

    /** --- Save module without clearing fields --- */
    private fun saveAirPollutionData(partial: Boolean = false) {
        val airPollution = collectAirPollutionInput() ?: return
        currentAirPollution = airPollution
        smrViewModel.updateAirPollution(airPollution)

        val message = if (partial) "Module saved. You can complete it later."
        else "Air Pollution data saved successfully."

        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    /** --- Preload fields if editing existing data --- */
    private fun populateFields(data: AirPollution) = with(binding) {
        etEquipmentName.setText(data.processEquipment)
        etLocation.setText(data.location)
        etHoursOperation.setText(data.hoursOperation)
        etFuelEquipment.setText(data.fuelEquipment)
        etFuelUsed.setText(data.fuelUsed)
        etFuelQuantity.setText(data.fuelQuantity)
        etFuelHours.setText(data.fuelHours)
        etPcfName.setText(data.pcfName)
        etPcfLocation.setText(data.pcfLocation)
        etPcfHours.setText(data.pcfHours)
        etTotalElectricity.setText(data.totalElectricity)
        etOverheadCost.setText(data.overheadCost)
        etEmissionDescription.setText(data.emissionDescription)
        etEmissionDate.setText(data.emissionDate)
        etFlowRate.setText(data.flowRate)
        etCO.setText(data.co)
        etNOx.setText(data.nox)
        etParticulates.setText(data.particulates)
    }

    /** --- Clear input fields (only after Add) --- */
    private fun clearFields() = with(binding) {
        etEquipmentName.text?.clear()
        etLocation.text?.clear()
        etHoursOperation.text?.clear()
        etFuelEquipment.text?.clear()
        etFuelUsed.text?.clear()
        etFuelQuantity.text?.clear()
        etFuelHours.text?.clear()
        etPcfName.text?.clear()
        etPcfLocation.text?.clear()
        etPcfHours.text?.clear()
        etTotalElectricity.text?.clear()
        etOverheadCost.text?.clear()
        etEmissionDescription.text?.clear()
        etEmissionDate.text?.clear()
        etFlowRate.text?.clear()
        etCO.text?.clear()
        etNOx.text?.clear()
        etParticulates.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
