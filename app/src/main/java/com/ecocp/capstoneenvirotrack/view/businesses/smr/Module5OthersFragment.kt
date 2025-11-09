package com.ecocp.capstoneenvirotrack.view.businesses.smr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentModule5OthersBinding
import com.ecocp.capstoneenvirotrack.model.Others
import com.ecocp.capstoneenvirotrack.viewmodel.SmrViewModel
import com.google.android.material.snackbar.Snackbar

class Module5OthersFragment : Fragment() {

    private var _binding: FragmentModule5OthersBinding? = null
    private val binding get() = _binding!!
    private val smrViewModel: SmrViewModel by activityViewModels()
    private var currentOthers: Others? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModule5OthersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Preload existing Others data if any
        smrViewModel.smr.value?.others?.let {
            currentOthers = it
            populateFields(it)
        }

        /** --- Save partial or full module --- */
        binding.btnSaveOthers.setOnClickListener {
            saveOthersData(partial = true)
        }

        /** --- Navigate to SMR Summary --- */
        binding.btnNextSummary.setOnClickListener {
            saveOthersData(partial = true)
            findNavController().navigate(R.id.action_module5OthersFragment_to_smrSummaryFragment)
        }
    }

    /** --- Save data to ViewModel --- */
    private fun saveOthersData(partial: Boolean = false) {
        val others = Others(
            accidentDate = binding.etAccidentDate.text?.toString()?.trim().orEmpty(),
            accidentArea = binding.etAccidentArea.text?.toString()?.trim().orEmpty(),
            findings = binding.etFindings.text?.toString()?.trim().orEmpty(),
            actionsTaken = binding.etActions.text?.toString()?.trim().orEmpty(),
            remarks = binding.etRemarks.text?.toString()?.trim().orEmpty(),
            trainingDate = binding.etTrainingDate.text?.toString()?.trim().orEmpty(),
            trainingDescription = binding.etTrainingDescription.text?.toString()?.trim().orEmpty(),
            personnelTrained = binding.etPersonnelTrained.text?.toString()?.trim().orEmpty()
        )

        currentOthers = others
        smrViewModel.updateOthers(others)

        val message = if (partial) "Module 5 saved. You can complete it later."
        else "Module 5 data saved successfully!"
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    /** --- Preload existing data into input fields --- */
    private fun populateFields(data: Others) = with(binding) {
        etAccidentDate.setText(data.accidentDate)
        etAccidentArea.setText(data.accidentArea)
        etFindings.setText(data.findings)
        etActions.setText(data.actionsTaken)
        etRemarks.setText(data.remarks)
        etTrainingDate.setText(data.trainingDate)
        etTrainingDescription.setText(data.trainingDescription)
        etPersonnelTrained.setText(data.personnelTrained)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
