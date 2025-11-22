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
import com.ecocp.capstoneenvirotrack.databinding.FragmentSmrModule2HazardousWasteBinding
import com.ecocp.capstoneenvirotrack.model.HazardousWaste
import com.ecocp.capstoneenvirotrack.viewmodel.SmrViewModel

class Module2HazardousWasteFragment : Fragment() {

    private var _binding: FragmentSmrModule2HazardousWasteBinding? = null
    private val binding get() = _binding!!

    private val smrViewModel: SmrViewModel by activityViewModels()
    private val hazardousWasteList = mutableListOf<HazardousWaste>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSmrModule2HazardousWasteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        /** --- Load existing entries from ViewModel if any --- */
        smrViewModel.smr.value?.hazardousWastes?.let { hazardousWasteList.addAll(it) }

        /** ✅ Add Hazardous Waste Entry */
        binding.btnAddHazardousWaste.setOnClickListener {
            val waste = collectInput()
            if (waste != null) {
                hazardousWasteList.add(waste)
                smrViewModel.updateHazardousWastes(hazardousWasteList)
                Toast.makeText(requireContext(), "Hazardous waste entry added", Toast.LENGTH_SHORT).show()
                clearFields()
            }
        }

        /** ✅ Save Module (partial save) */
        binding.SaveModule.setOnClickListener {
            saveModule(partial = true)
        }

        /** ✅ Proceed to Module 3: Water Pollution */
        binding.btnNextModule3.setOnClickListener {
            saveModule(partial = true)
            findNavController().navigate(R.id.action_module2HazardousWasteFragment_to_module3WaterPollutionFragment)
        }
    }

    /** --- Save current module data into ViewModel and update progress --- */
    private fun saveModule(partial: Boolean) {
        smrViewModel.updateHazardousWastes(hazardousWasteList)
        if (partial) {
            Toast.makeText(requireContext(), "Module saved. You can complete it later.", Toast.LENGTH_SHORT).show()
        }
    }

    /** ✅ Collect input fields safely */
    private fun collectInput(): HazardousWaste? {
        val commonName = binding.etCommonName.text?.toString()?.trim().orEmpty()
        val casNo = binding.etCasNo.text?.toString()?.trim().orEmpty()
        val tradeName = binding.etTradeName.text?.toString()?.trim().orEmpty()
        val hwNo = binding.etHwNo.text?.toString()?.trim().orEmpty()
        val hwClass = binding.etHwClass.text?.toString()?.trim().orEmpty()
        val hwGenerated = binding.etHwGenerated.text?.toString()?.trim().orEmpty()
        val storageMethod = binding.etStorageMethod.text?.toString()?.trim().orEmpty()
        val transporter = binding.etTransporter.text?.toString()?.trim().orEmpty()
        val treater = binding.etTreater.text?.toString()?.trim().orEmpty()
        val disposalMethod = binding.etDisposalMethod.text?.toString()?.trim().orEmpty()

        if (commonName.isEmpty() || hwNo.isEmpty() || hwGenerated.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill out required fields", Toast.LENGTH_SHORT).show()
            return null
        }

        return HazardousWaste(
            commonName = commonName,
            casNo = casNo,
            tradeName = tradeName,
            hwNo = hwNo,
            hwClass = hwClass,
            hwGenerated = hwGenerated,
            storageMethod = storageMethod,
            transporter = transporter,
            treater = treater,
            disposalMethod = disposalMethod
        )
    }

    /** ✅ Clear input fields after adding entry */
    private fun clearFields() = with(binding) {
        etCommonName.text?.clear()
        etCasNo.text?.clear()
        etTradeName.text?.clear()
        etHwNo.text?.clear()
        etHwClass.text?.clear()
        etHwGenerated.text?.clear()
        etStorageMethod.text?.clear()
        etTransporter.text?.clear()
        etTreater.text?.clear()
        etDisposalMethod.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
