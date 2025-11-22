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

            val bundle = Bundle().apply {
                putString("companyName", selected.companyName)
                putString("serviceTitle", selected.serviceTitle)
                putString("origin", selected.origin)
                putString("dateRequested", selected.dateRequested)  // ✔ FIXED
                putString("providerName", selected.providerName)
                putString("providerContact", selected.providerContact)
                putString("status", selected.status)
                putString("notes", selected.notes.ifEmpty { selected.compliance })

                // attachments — send first or whole list
                putString(
                    "attachment",
                    selected.attachments?.firstOrNull()
                        ?: selected.imageUrl   // fallback to the image path (local file)
                )
            }

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
                    providerName = "John Doe",
                    providerContact = "(0917) 123-4567",
                    serviceTitle = "Waste Disposal for Client A",
                    status = "Pending",
                    origin = "Mandaue City",
                    dateRequested = "Feb 8, 2025",
                    notes = "Pickup at rear gate.",
                    attachments = listOf("/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png")
                ),
                ServiceRequest(
                    id = "2",
                    clientName = "Client B",
                    companyName = "Jollibee",
                    providerName = "Maria Reyes",
                    providerContact = "(0908) 654-2221",
                    serviceTitle = "Water Quality Testing",
                    status = "In Progress",
                    origin = "Cebu City",
                    dateRequested = "Feb 7, 2025",
                    notes = "Bring sample bottles.",
                    attachments = listOf("/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png")
                ),
                ServiceRequest(
                    id = "3",
                    clientName = "Client C",
                    companyName = "Starbucks",
                    providerName = "Leo Cruz",
                    providerContact = "(0935) 888-1199",
                    serviceTitle = "Hazardous Waste Collection",
                    status = "Completed",
                    origin = "Lapu-Lapu City",
                    dateRequested = "Feb 5, 2025",
                    notes = "Hazardous waste stored behind kitchen.",
                    attachments = listOf("/mnt/data/16bb7df0-6158-4979-b2a0-49574fc2bb5e.png")
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
