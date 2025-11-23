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
        // ServiceRequestAdapter(requests, isActiveTasks, onActionClick)
        adapter = ServiceRequestAdapter(activeTasks, isActiveTasks = true) { selected ->
            // When in Active Tasks, the action is "Update Status"
            val bundle = Bundle().apply {
                putString("requestId", selected.id)                 // use id so detail/update screens can fetch doc
                putString("companyName", selected.companyName)
                putString("serviceTitle", selected.serviceTitle)
                putString("status", selected.status)
                putString("providerName", selected.providerName)
                putString("providerContact", selected.providerContact)
                // pass an attachment (first or fallback to dev path)
                putString(
                    "attachment",
                    selected.attachments?.firstOrNull()
                        ?: selected.imageUrl.ifEmpty { "/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png" }
                )
            }

            // navigate to the Task Update screen (use action/id you have in nav_graph)
            findNavController().navigate(R.id.SP_TaskUpdateDetails, bundle)
        }

        binding.recyclerActiveTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerActiveTasks.adapter = adapter
    }


    private fun loadActiveTasks() {
        activeTasks.addAll(
            listOf(
                ServiceRequest(
                    id = "1",
                    bookingId = null,
                    clientName = "Dunkin",
                    companyName = "Dunkin Donuts",
                    providerName = "N/A",
                    providerContact = "N/A",
                    serviceTitle = "Waste Disposal for Dunkin",
                    status = "Pending",
                    origin = "Mandaue City",
                    destination = "TSD Facility",
                    dateRequested = "Feb 1, 2025",
                    wasteType = "B201 - Sulfuric Acid",
                    quantity = "150 liters",
                    packaging = "Sealed drums",
                    notes = "Handle with PPE",
                    compliance = "In Compliance",
                    attachments = listOf("/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png"),
                    imageUrl = "/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png"
                ),
                ServiceRequest(
                    id = "2",
                    bookingId = null,
                    clientName = "McDonald's",
                    companyName = "McDonald's",
                    providerName = "N/A",
                    providerContact = "N/A",
                    serviceTitle = "Waste Disposal for McDonald's",
                    status = "In Progress",
                    origin = "Cebu City",
                    destination = "TSD Facility",
                    dateRequested = "Feb 5, 2025",
                    wasteType = "Mixed Waste",
                    quantity = "200 kg",
                    packaging = "Double-bagged",
                    notes = "",
                    compliance = "In Compliance",
                    attachments = listOf("/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png"),
                    imageUrl = "/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png"
                ),
                ServiceRequest(
                    id = "3",
                    bookingId = null,
                    clientName = "Jollibee",
                    companyName = "Jollibee Foods Corp",
                    providerName = "N/A",
                    providerContact = "N/A",
                    serviceTitle = "Waste Disposal for Jollibee",
                    status = "Completed",
                    origin = "Lapu-Lapu City",
                    destination = "TSD Facility",
                    dateRequested = "Feb 10, 2025",
                    wasteType = "Kitchen Waste",
                    quantity = "300 kg",
                    packaging = "Plastic bins",
                    notes = "",
                    compliance = "In Compliance",
                    attachments = listOf("/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png"),
                    imageUrl = "/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png"
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
