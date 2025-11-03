package com.ecocp.capstoneenvirotrack.view.businesses.smr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            binding.etOwner to "Please enter owner or company name."
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
