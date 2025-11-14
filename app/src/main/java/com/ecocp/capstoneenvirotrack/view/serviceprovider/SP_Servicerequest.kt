package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.databinding.FragmentSpServicerequestBinding
import com.ecocp.capstoneenvirotrack.model.ServiceRequest
import com.ecocp.capstoneenvirotrack.view.serviceprovider.adapters.ServiceRequestAdapter
import com.ecocp.capstoneenvirotrack.R

class SP_Servicerequest : Fragment() {

    private var _binding: FragmentSpServicerequestBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ServiceRequestAdapter
    private val requests = mutableListOf<ServiceRequest>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpServicerequestBinding.inflate(inflater, container, false)

        setupRecyclerView()
        loadRequests()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = ServiceRequestAdapter(requests) { selected ->
            // 🟢 Pass selected item details to the next screen
            val bundle = Bundle().apply {
                putString("companyName", selected.companyName)
                putString("serviceType", selected.serviceTitle)
                putString("location", "Mandaue City") // Example static field
                putString("dateRequested", "Feb 8, 2025") // Example static field
                putString("status", selected.status)
            }

            // 🟢 Navigate to details fragment
            findNavController().navigate(
                R.id.action_SP_Servicerequest_to_SP_ServiceRequestDetails,
                bundle
            )
        }

        binding.recyclerRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRequests.adapter = adapter
    }

    private fun loadRequests() {
        requests.addAll(
            listOf(
                ServiceRequest(
                    id = "1",
                    clientName = "Client A",
                    companyName = "McDonald’s",
                    serviceTitle = "Waste Disposal for Client A",
                    status = "Pending",
                    compliance = "In Compliance"
                ),
                ServiceRequest(
                    id = "2",
                    clientName = "Client B",
                    companyName = "Jollibee",
                    serviceTitle = "Water Quality Testing",
                    status = "In Progress",
                    compliance = "In Compliance"
                ),
                ServiceRequest(
                    id = "3",
                    clientName = "Client C",
                    companyName = "Starbucks",
                    serviceTitle = "Hazardous Waste Collection",
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
