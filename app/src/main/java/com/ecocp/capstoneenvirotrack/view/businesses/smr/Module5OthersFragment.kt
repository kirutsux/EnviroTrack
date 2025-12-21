package com.ecocp.capstoneenvirotrack.view.businesses.smr

import android.app.DatePickerDialog
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
import com.ecocp.capstoneenvirotrack.databinding.FragmentModule5OthersBinding
import com.ecocp.capstoneenvirotrack.model.Others
import com.ecocp.capstoneenvirotrack.viewmodel.SmrViewModel
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

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

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Setup dropdowns
        setupDropdowns()

        // Setup date pickers
        setupDatePickers()

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

    /** --- Setup AutoCompleteTextView Dropdowns with Custom Input Support --- */
    private fun setupDropdowns() {
        // Accident Area (with custom input support)
        val areaArray = resources.getStringArray(R.array.accident_area_types)
        val areaAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            areaArray
        )
        val etAccidentArea = binding.etAccidentArea as AutoCompleteTextView
        etAccidentArea.setAdapter(areaAdapter)
        etAccidentArea.threshold = 1
        etAccidentArea.inputType = android.text.InputType.TYPE_CLASS_TEXT
        etAccidentArea.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                etAccidentArea.showDropDown()
            }
        }

        // Findings (with custom input support)
        val findingsArray = resources.getStringArray(R.array.incident_findings_categories)
        val findingsAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            findingsArray
        )
        val etFindings = binding.etFindings as AutoCompleteTextView
        etFindings.setAdapter(findingsAdapter)
        etFindings.threshold = 1
        etFindings.inputType = android.text.InputType.TYPE_CLASS_TEXT
        etFindings.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                etFindings.showDropDown()
            }
        }

        // Actions Taken (with custom input support)
        val actionsArray = resources.getStringArray(R.array.corrective_actions_list)
        val actionsAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            actionsArray
        )
        val etActions = binding.etActions as AutoCompleteTextView
        etActions.setAdapter(actionsAdapter)
        etActions.threshold = 1
        etActions.inputType = android.text.InputType.TYPE_CLASS_TEXT
        etActions.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                etActions.showDropDown()
            }
        }

        // Training Description (with custom input support)
        val trainingArray = resources.getStringArray(R.array.training_types_list)
        val trainingAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            trainingArray
        )
        val etTrainingDescription = binding.etTrainingDescription as AutoCompleteTextView
        etTrainingDescription.setAdapter(trainingAdapter)
        etTrainingDescription.threshold = 1
        etTrainingDescription.inputType = android.text.InputType.TYPE_CLASS_TEXT
        etTrainingDescription.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                etTrainingDescription.showDropDown()
            }
        }
    }

    /** --- Setup DatePicker for Date Fields --- */
    private fun setupDatePickers() {
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())

        // Accident Date DatePicker
        binding.etAccidentDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    calendar.set(selectedYear, selectedMonth, selectedDay)
                    val selectedDate = dateFormat.format(calendar.time)
                    binding.etAccidentDate.setText(selectedDate)
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        // Training Date DatePicker
        binding.etTrainingDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    calendar.set(selectedYear, selectedMonth, selectedDay)
                    val selectedDate = dateFormat.format(calendar.time)
                    binding.etTrainingDate.setText(selectedDate)
                },
                year, month, day
            )
            datePickerDialog.show()
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