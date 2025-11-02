package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentSpCompletedServicesBinding
import com.ecocp.capstoneenvirotrack.model.ServiceRequest
import com.ecocp.capstoneenvirotrack.view.serviceprovider.adapters.CompletedServiceAdapter

class SP_CompletedServices : Fragment() {

    private var _binding: FragmentSpCompletedServicesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CompletedServiceAdapter
    private val completedList = mutableListOf<ServiceRequest>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpCompletedServicesBinding.inflate(inflater, container, false)

        setupRecyclerView()
        loadCompletedData()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = CompletedServiceAdapter(completedList) { selected ->
            // 🔹 When “View Report” is clicked, go to SP_ServiceReport
            val bundle = Bundle().apply {
                putString("companyName", selected.companyName)
                putString("serviceTitle", selected.serviceTitle)
                putString("status", selected.status)
                putString("compliance", selected.compliance)
            }

            findNavController().navigate(R.id.SP_ServiceReport, bundle)
        }

        binding.recyclerCompleted.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCompleted.adapter = adapter
    }

    private fun loadCompletedData() {
        completedList.addAll(
            listOf(
                ServiceRequest(
                    id = "1",
                    clientName = "Client A",
                    companyName = "Dunkin Donuts",
                    serviceTitle = "Waste Disposal for Dunkin",
                    status = "Completed",
                    compliance = "In Compliance"
                ),
                ServiceRequest(
                    id = "2",
                    clientName = "Client B",
                    companyName = "McDonald's",
                    serviceTitle = "Oil Collection for McDonald's",
                    status = "Completed",
                    compliance = "In Compliance"
                ),
                ServiceRequest(
                    id = "3",
                    clientName = "Client C",
                    companyName = "Jollibee Foods Corp",
                    serviceTitle = "Waste Audit for Jollibee",
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
