package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentSpActiveTasksBinding
import com.ecocp.capstoneenvirotrack.model.ServiceRequest
import com.ecocp.capstoneenvirotrack.view.serviceprovider.adapters.ServiceRequestAdapter

class SP_ActiveTasks : Fragment() {

    private var _binding: FragmentSpActiveTasksBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ServiceRequestAdapter
    private val activeTasks = mutableListOf<ServiceRequest>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpActiveTasksBinding.inflate(inflater, container, false)

        setupRecyclerView()
        loadActiveTasks()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = ServiceRequestAdapter(activeTasks) { selected ->
            val bundle = Bundle().apply {
                putString("companyName", selected.companyName)
                putString("serviceTitle", selected.serviceTitle)
                putString("status", selected.status)
            }
            findNavController().navigate(R.id.SP_TaskUpdateDetails, bundle)
        }

        // ✅ These lines should be INSIDE the function
        binding.recyclerActiveTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerActiveTasks.adapter = adapter
    }

    private fun loadActiveTasks() {
        activeTasks.addAll(
            listOf(
                ServiceRequest(
                    id = "1",
                    clientName = "Dunkin",
                    companyName = "Dunkin Donuts",
                    serviceTitle = "Waste Disposal for Dunkin",
                    status = "Pending",
                    compliance = "In Compliance"
                ),
                ServiceRequest(
                    id = "2",
                    clientName = "McDonald's",
                    companyName = "McDonald's",
                    serviceTitle = "Waste Disposal for McDonald's",
                    status = "In Progress",
                    compliance = "In Compliance"
                ),
                ServiceRequest(
                    id = "3",
                    clientName = "Jollibee",
                    companyName = "Jollibee Foods Corp",
                    serviceTitle = "Waste Disposal for Jollibee",
                    status = "Completed",
                    compliance = "In Compliance"
                )
            )
        )
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
