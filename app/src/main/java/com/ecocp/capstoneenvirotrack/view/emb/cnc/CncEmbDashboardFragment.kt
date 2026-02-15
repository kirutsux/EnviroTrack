package com.ecocp.capstoneenvirotrack.view.emb.cnc

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.CncEmbAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentCncEmbDashboardBinding
import com.ecocp.capstoneenvirotrack.model.CncApplication
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class CncEmbDashboardFragment : Fragment() {

    private var _binding: FragmentCncEmbDashboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private lateinit var adapter: CncEmbAdapter
    private val cncList = mutableListOf<CncApplication>()
    private val filteredList = mutableListOf<CncApplication>()
    private var selectedStatus: String = "All"

    private var snapshotListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCncEmbDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CncEmbAdapter(filteredList) { selectedApp ->
            val bundle = Bundle().apply { putString("applicationId", selectedApp.applicationId) }
            findNavController().navigate(
                R.id.action_cncEmbDashboardFragment_to_cncReviewDetailsFragment,
                bundle
            )
        }

        binding.recyclerEmbCncList.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerEmbCncList.adapter = adapter

        setupSpinner()
        setupSearchBar()

        loadAllCncApplications()
    }

    private fun setupSpinner() {
        val statusOptions = listOf("All", "Pending", "Approved", "Rejected")
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, statusOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = spinnerAdapter

        binding.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                selectedStatus = statusOptions[position]
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupSearchBar() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadAllCncApplications() {
        snapshotListener = db.collection("cnc_applications")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("EMB CNC", "Error loading CNC apps: ${e.message}")
                    return@addSnapshotListener
                }

                cncList.clear()
                snapshots?.documents?.forEach { doc ->
                    val data = doc.data ?: return@forEach

                    val submittedTimestamp = data["submittedTimestamp"] as? Timestamp
                    if (submittedTimestamp != null) { // Only include CNCs that have been submitted
                        val cnc = CncApplication(
                            applicationId = doc.id,
                            companyName = data["companyName"] as? String,
                            projectTitle = data["projectTitle"] as? String,
                            projectLocation = data["projectLocation"] as? String,
                            status = data["status"] as? String,
                            submittedTimestamp = submittedTimestamp
                        )
                        cncList.add(cnc)
                    }
                }

                cncList.sortByDescending { it.submittedTimestamp }
                applyFilters()
            }
    }
    
    private fun applyFilters() {
        // 🛡️ Prevent crash if fragment view is already destroyed
        val safeBinding = _binding ?: return

        val query = safeBinding.etSearch.text.toString().trim().lowercase()
        filteredList.clear()

        cncList.forEach { app ->
            val matchesStatus = selectedStatus == "All" ||
                    app.status.equals(selectedStatus, ignoreCase = true)
            val matchesSearch = query.isEmpty() || listOfNotNull(
                app.companyName,
                app.projectTitle,
                app.projectLocation
            ).any { it.lowercase().contains(query) }

            if (matchesStatus && matchesSearch) {
                filteredList.add(app)
            }
        }

        if (filteredList.isEmpty()) {
            safeBinding.txtNoApplications.visibility = View.VISIBLE
        } else {
            safeBinding.txtNoApplications.visibility = View.GONE
        }

        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snapshotListener?.remove() // stop listening when fragment is destroyed
        snapshotListener = null
        _binding = null
    }
}
