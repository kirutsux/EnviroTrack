package com.ecocp.capstoneenvirotrack.view.all

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.adapter.ChecklistAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentChecklistBinding
import com.ecocp.capstoneenvirotrack.model.ChecklistItem
import com.ecocp.capstoneenvirotrack.viewmodel.ChecklistViewModel
import com.google.firebase.auth.FirebaseAuth
import com.ecocp.capstoneenvirotrack.R
import java.util.*

class ChecklistFragment : Fragment() {

    private var _binding: FragmentChecklistBinding? = null
    private val binding get() = _binding!!
    private val checklistViewModel: ChecklistViewModel by activityViewModels()
    private lateinit var adapter: ChecklistAdapter
    private var currentFilter: String = "All"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChecklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilterSpinner()
        observeChecklistItems()
        setupAddItemButton()
    }

    private fun setupRecyclerView() {
        adapter = ChecklistAdapter(
            onStatusChange = { itemId, newStatus ->
                checklistViewModel.updateItemStatus(itemId, newStatus)
                updateProgressBar()
            },
            onItemLongClick = { item -> showEditDeleteDialog(item) } // <-- Long press callback
        )

        binding.recyclerViewChecklist.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewChecklist.adapter = adapter
    }

    private fun setupFilterSpinner() {
        val statusOptions = resources.getStringArray(R.array.checklist_status_filter)
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilterStatus.adapter = spinnerAdapter

        binding.spinnerFilterStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentFilter = statusOptions[position]
                filterChecklistItems()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun observeChecklistItems() {
        checklistViewModel.checklistItems.observe(viewLifecycleOwner) { items ->
            filterChecklistItems(items)
            updateProgressBar()
        }
    }

    private fun filterChecklistItems(items: List<ChecklistItem>? = checklistViewModel.checklistItems.value) {
        val filteredList = when (currentFilter) {
            "All" -> items.orEmpty()
            else -> items.orEmpty().filter { it.status == currentFilter }
        }
        adapter.submitList(filteredList)
    }

    private fun setupAddItemButton() {
        binding.btnAddItem.setOnClickListener {
            val name = binding.etItemName.text.toString().trim()
            val desc = binding.etItemDescription.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a checklist name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            checklistViewModel.addItem(name, desc)

            binding.etItemName.text?.clear()
            binding.etItemDescription.text?.clear()
        }
    }

    private fun showEditDeleteDialog(item: ChecklistItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_delete_checklist_item, null)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogItemName)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDialogItemDescription)

        etName.setText(item.name)
        etDesc.setText(item.description)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit or Delete Item")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newName = etName.text.toString().trim()
                val newDesc = etDesc.text.toString().trim()
                if (newName.isNotEmpty()) {
                    checklistViewModel.updateItem(item.id, newName, newDesc)
                }
            }
            .setNegativeButton("Delete") { _, _ ->
                checklistViewModel.deleteItem(item.id)
            }
            .setNeutralButton("Cancel", null)
            .create()

        dialog.show()
    }


    private fun updateProgressBar() {
        val percent = checklistViewModel.getCompletionPercentage()
        binding.progressBarChecklist.progress = percent
        binding.tvProgress.text = "$percent%"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
