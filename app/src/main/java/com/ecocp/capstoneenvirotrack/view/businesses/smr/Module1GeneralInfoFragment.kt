package com.ecocp.capstoneenvirotrack.view.businesses.smr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentModule1GeneralInfoBinding
import com.ecocp.capstoneenvirotrack.model.GeneralInfo
import com.ecocp.capstoneenvirotrack.viewmodel.SmrViewModel
import com.google.android.material.snackbar.Snackbar

class Module1GeneralInfoFragment : Fragment() {

    private var _binding: FragmentModule1GeneralInfoBinding? = null
    private val binding get() = _binding!!
    private val smrViewModel: SmrViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModule1GeneralInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Setup dropdowns with custom input support
        setupDropdowns()

        /** ✅ Save Module without clearing fields */
        binding.SaveModule.setOnClickListener {
            if (validateInputs()) saveGeneralInfo(partial = true)
        }

        /** ✅ Proceed to Module 2 (Hazardous Waste) */
        binding.btnNextModule2.setOnClickListener {
            if (validateInputs()) {
                saveGeneralInfo(partial = true)
                findNavController().navigate(R.id.action_module1GeneralInfoFragment_to_module2HazardousWasteFragment)
            }
        }

        /** --- Pre-fill fields if ViewModel has data --- */
        prefillFields()
    }

    /** --- Setup AutoCompleteTextView Dropdowns with Custom Input Support --- */
    private fun setupDropdowns() {
        // Type of Business / Industry Dropdown (with custom input)
        val industryArray = resources.getStringArray(R.array.emb_industry_categories)
        val industryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            industryArray
        )
        val etTypeOfBusiness = binding.etTypeOfBusiness as AutoCompleteTextView
        etTypeOfBusiness.setAdapter(industryAdapter)
        // Set threshold to 0 to show all suggestions on focus, or 1 to require typing
        etTypeOfBusiness.threshold = 1
        // Allow freeform input (user can type anything, not just from dropdown)
        etTypeOfBusiness.inputType = android.text.InputType.TYPE_CLASS_TEXT
        // Optional: Show dropdown when field is focused
        etTypeOfBusiness.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                etTypeOfBusiness.showDropDown()
            }
        }

        // Legal Classification Dropdown (with custom input)
        val legalArray = resources.getStringArray(R.array.legal_classification_types)
        val legalAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            legalArray
        )
        val etLegalClassification = binding.etLegalClassification as AutoCompleteTextView
        etLegalClassification.setAdapter(legalAdapter)
        // Set threshold to 0 to show all suggestions on focus, or 1 to require typing
        etLegalClassification.threshold = 1
        // Allow freeform input
        etLegalClassification.inputType = android.text.InputType.TYPE_CLASS_TEXT
        // Optional: Show dropdown when field is focused
        etLegalClassification.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                etLegalClassification.showDropDown()
            }
        }
    }

    /** --- Save data to ViewModel and update progress automatically --- */
    private fun saveGeneralInfo(partial: Boolean) {
        val generalInfo = GeneralInfo(
            establishmentName = binding.etEstablishmentName.text.toString().trim(),
            address = binding.etAddress.text.toString().trim(),
            ownerName = binding.etOwner.text.toString().trim(),
            phone = binding.etPhone.text.toString().trim(),
            email = binding.etEmail.text.toString().trim(),
            typeOfBusiness = binding.etTypeOfBusiness.text.toString().trim(),
            ceoName = binding.etCeoName.text.toString().trim(),
            ceoPhone = binding.etCeoPhone.text.toString().trim(),
            ceoEmail = binding.etCeoEmail.text.toString().trim(),
            pcoName = binding.etPcoName.text.toString().trim(),
            pcoPhone = binding.etPcoPhone.text.toString().trim(),
            pcoEmail = binding.etPcoEmail.text.toString().trim(),
            pcoAccreditationNo = binding.etPcoAccNo.text.toString().trim(),
            legalClassification = binding.etLegalClassification.text.toString().trim()
        )

        // Update ViewModel (automatically triggers dashboard progress update)
        smrViewModel.updateGeneralInfo(generalInfo)

        if (partial) {
            Snackbar.make(binding.root, "Module saved. You can complete it later.", Snackbar.LENGTH_SHORT).show()
        }
    }

    /** --- Validate required fields --- */
    private fun validateInputs(): Boolean {
        val requiredFields = mapOf(
            binding.etEstablishmentName to "Please enter establishment name.",
            binding.etAddress to "Please enter address.",
            binding.etOwner to "Please enter owner or company name.",
            binding.etTypeOfBusiness to "Please enter or select type of business.",
            binding.etLegalClassification to "Please enter or select legal classification."
        )

        for ((field, message) in requiredFields) {
            if (field.text.isNullOrEmpty()) {
                showSnack(message)
                field.requestFocus()
                return false
            }
        }
        return true
    }

    /** --- Pre-fill existing data from ViewModel --- */
    private fun prefillFields() {
        smrViewModel.smr.value?.generalInfo?.let { info ->
            binding.etEstablishmentName.setText(info.establishmentName)
            binding.etAddress.setText(info.address)
            binding.etOwner.setText(info.ownerName)
            binding.etPhone.setText(info.phone)
            binding.etEmail.setText(info.email)
            binding.etTypeOfBusiness.setText(info.typeOfBusiness)
            binding.etCeoName.setText(info.ceoName)
            binding.etCeoPhone.setText(info.ceoPhone)
            binding.etCeoEmail.setText(info.ceoEmail)
            binding.etPcoName.setText(info.pcoName)
            binding.etPcoPhone.setText(info.pcoPhone)
            binding.etPcoEmail.setText(info.pcoEmail)
            binding.etPcoAccNo.setText(info.pcoAccreditationNo)
            binding.etLegalClassification.setText(info.legalClassification)
        }
    }

    private fun showSnack(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}