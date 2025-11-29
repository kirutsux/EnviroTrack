package com.ecocp.capstoneenvirotrack.view.all

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.adapter.ChecklistAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentChecklistBinding
import com.ecocp.capstoneenvirotrack.model.ChecklistItem
import com.ecocp.capstoneenvirotrack.viewmodel.ChecklistViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class ChecklistFragment : Fragment() {

    private var _binding: FragmentChecklistBinding? = null
    private val binding get() = _binding!!
    private val checklistViewModel: ChecklistViewModel by activityViewModels()
    private lateinit var adapter: ChecklistAdapter

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
        observeChecklistItems()
        setupAddItemButton()
    }

    private fun setupRecyclerView() {
        adapter = ChecklistAdapter { itemId, newStatus ->
            checklistViewModel.updateItemStatus(itemId, newStatus)
            updateProgressBar()
        }
        binding.recyclerViewChecklist.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewChecklist.adapter = adapter
    }

    private fun observeChecklistItems() {
        checklistViewModel.checklistItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items.toList()) // to avoid RecyclerView diff issues
            updateProgressBar()
        }
    }

    private fun setupAddItemButton() {
        binding.btnAddItem.setOnClickListener {
            val name = binding.etItemName.text.toString().trim()
            val desc = binding.etItemDescription.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a checklist name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create a ChecklistItem with userId
            val item = ChecklistItem(
                id = UUID.randomUUID().toString(),
                name = name,
                description = desc,
                userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            )

            // Add item to ViewModel / Firestore
            checklistViewModel.addItem(name, desc)

            // Clear input fields
            binding.etItemName.text?.clear()
            binding.etItemDescription.text?.clear()
        }

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
