package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentSpCompletedServicesBinding
import com.ecocp.capstoneenvirotrack.model.ServiceRequest
import com.ecocp.capstoneenvirotrack.view.serviceprovider.adapters.CompletedServiceAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        setupSwipeToRefresh()
        loadCompletedDataAsync()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = CompletedServiceAdapter(completedList) { selected ->
            // prepare bundle for detail screen
            val bundle = Bundle().apply {
                putString("companyName", selected.companyName)
                putString("serviceTitle", selected.serviceTitle)
                putString("status", selected.status)
                putString("compliance", selected.compliance)
                putString("clientName", selected.clientName)
                putString("requestId", selected.id)
            }

            // try to navigate using explicit action id if available, else navigate by destination id
            try {
                // prefer safe action id if you created it in nav graph:
                findNavController().navigate(R.id.SP_ServiceReport, bundle)
            } catch (ex: IllegalArgumentException) {
                // fallback to using fragment id as you had before (works if that id exists)
                findNavController().navigate(R.id.SP_ServiceReport, bundle)
            }
        }

        binding.recyclerCompleted.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCompleted.adapter = adapter
    }

    private fun setupSwipeToRefresh() {
        // If your layout doesn't have a SwipeRefreshLayout, add one in XML and change the id accordingly.
        // Here we assume you added swipeRefresh inside the card as in the enhanced layout.
        binding.swipeRefresh.setOnRefreshListener {
            // simulate refresh
            loadCompletedDataAsync(refresh = true)
        }
    }

    private fun loadCompletedDataAsync(refresh: Boolean = false) {
        // show progress (optional)
        binding.progressLoading?.visibility = View.VISIBLE
        binding.txtEmptyState?.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            // simulate network / DB work on IO
            val data = withContext(Dispatchers.IO) {
                // simulate delay only when actually refreshing
                if (refresh) delay(600)
                // return the sample data (replace with real fetch)
                buildSampleData()
            }

            // update UI on main thread
            completedList.clear()
            completedList.addAll(data)

            // Prefer adapter.setData() if your adapter exposes it; otherwise notifyDataSetChanged
            try {
                val setDataMethod = adapter::class.java.getMethod("setData", List::class.java)
                setDataMethod.invoke(adapter, data)
            } catch (_: Exception) {
                // fallback
                adapter.notifyDataSetChanged()
            }

            binding.swipeRefresh.isRefreshing = false
            binding.progressLoading?.visibility = View.GONE

            binding.txtEmptyState.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun buildSampleData(): List<ServiceRequest> {
        return listOf(
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
