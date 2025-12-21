package com.ecocp.capstoneenvirotrack.view.businesses.smr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentSmrModule2HazardousWasteBinding
import com.ecocp.capstoneenvirotrack.model.HazardousWaste
import com.ecocp.capstoneenvirotrack.viewmodel.SmrViewModel
import com.ecocp.capstoneenvirotrack.viewmodel.SmrViewModelFactory

class Module2HazardousWasteFragment : Fragment() {

    private var _binding: FragmentSmrModule2HazardousWasteBinding? = null
    private val binding get() = _binding!!

    private val smrViewModel: SmrViewModel by activityViewModels {
        SmrViewModelFactory(requireActivity().application)
    }
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

        // Load existing entries from ViewModel if any
        smrViewModel.smr.value?.hazardousWastes?.let {
            hazardousWasteList.addAll(it)
        }

        // Setup dropdowns
        setupDropdowns()

        setupListeners()
    }

    /** --- Setup AutoCompleteTextView Dropdowns with Custom Input Support --- */
    private fun setupDropdowns() {
        // HW No. Dropdown (with custom input)
        val hwNoArray = resources.getStringArray(R.array.hw_no_list)
        val hwNoAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            hwNoArray
        )
        val etHwNo = binding.etHwNo as AutoCompleteTextView
        etHwNo.setAdapter(hwNoAdapter)
        etHwNo.threshold = 1
        etHwNo.inputType = android.text.InputType.TYPE_CLASS_TEXT
        etHwNo.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                etHwNo.showDropDown()
            }
        }

        // HW Class Dropdown (with custom input)
        val hwClassArray = resources.getStringArray(R.array.hw_class_list)
        val hwClassAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            hwClassArray
        )
        val etHwClass = binding.etHwClass as AutoCompleteTextView
        etHwClass.setAdapter(hwClassAdapter)
        etHwClass.threshold = 1
        etHwClass.inputType = android.text.InputType.TYPE_CLASS_TEXT
        etHwClass.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                etHwClass.showDropDown()
            }
        }

        // Storage Method Dropdown (with custom input)
        val storageArray = resources.getStringArray(R.array.storage_method_list)
        val storageAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            storageArray
        )
        val etStorageMethod = binding.etStorageMethod as AutoCompleteTextView
        etStorageMethod.setAdapter(storageAdapter)
        etStorageMethod.threshold = 1
        etStorageMethod.inputType = android.text.InputType.TYPE_CLASS_TEXT
        etStorageMethod.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                etStorageMethod.showDropDown()
            }
        }

        // Disposal Method Dropdown (with custom input)
        val disposalArray = resources.getStringArray(R.array.disposal_method_list)
        val disposalAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            disposalArray
        )
        val etDisposalMethod = binding.etDisposalMethod as AutoCompleteTextView
        etDisposalMethod.setAdapter(disposalAdapter)
        etDisposalMethod.threshold = 1
        etDisposalMethod.inputType = android.text.InputType.TYPE_CLASS_TEXT
        etDisposalMethod.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                etDisposalMethod.showDropDown()
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // Add a new Hazardous Waste entry
        binding.btnAddHazardousWaste.setOnClickListener {
            val waste = collectInput()
            waste?.let {
                if (!hazardousWasteList.contains(it)) {
                    hazardousWasteList.add(it)
                    smrViewModel.updateHazardousWastes(hazardousWasteList)
                    Toast.makeText(requireContext(), "Hazardous waste entry added", Toast.LENGTH_SHORT).show()
                    clearFields()
                } else {
                    Toast.makeText(requireContext(), "This entry already exists", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Save module partially
        binding.SaveModule.setOnClickListener { saveModule(partial = true) }

        // Proceed to Module 3
        binding.btnNextModule3.setOnClickListener {
            saveModule(partial = true)
            findNavController().navigate(R.id.action_module2HazardousWasteFragment_to_module3WaterPollutionFragment)
        }
    }

    /** Save current module data into ViewModel */
    private fun saveModule(partial: Boolean) {
        smrViewModel.updateHazardousWastes(hazardousWasteList)
        if (partial) {
            Toast.makeText(requireContext(), "Module saved. You can complete it later.", Toast.LENGTH_SHORT).show()
        }
    }

    /** Collect input fields safely */
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
            Toast.makeText(requireContext(), "Please fill out required fields (Common Name, HW No., Quantity)", Toast.LENGTH_SHORT).show()
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

    /** Clear input fields after adding entry */
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