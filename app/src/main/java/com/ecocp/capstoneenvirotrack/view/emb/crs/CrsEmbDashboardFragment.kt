package com.ecocp.capstoneenvirotrack.view.emb.crs

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
import com.ecocp.capstoneenvirotrack.adapter.CrsEmbAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentCrsEmbDashboardBinding
import com.ecocp.capstoneenvirotrack.model.CrsApplication
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class CrsEmbDashboardFragment : Fragment() {

    private var _binding: FragmentCrsEmbDashboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private lateinit var adapter: CrsEmbAdapter
    private val crsList = mutableListOf<CrsApplication>()
    private val filteredList = mutableListOf<CrsApplication>()
    private var selectedStatus: String = "All"

    private var snapshotListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCrsEmbDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CrsEmbAdapter(filteredList) { selectedApp ->
            val bundle = Bundle().apply {
                putString("applicationId", selectedApp.applicationId)
            }
            findNavController().navigate(
                R.id.action_crsEmbDashboardFragment_to_crsEmbReviewDetailsFragment,
                bundle
            )
        }

        binding.recyclerEmbCrsList.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerEmbCrsList.adapter = adapter

        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_embcrsDashboardFragment_to_embDashboardFragment)
        }

        setupSpinner()
        setupSearchBar()
        loadAllCrsApplications()
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

    private fun loadAllCrsApplications() {
        snapshotListener = db.collection("crs_applications")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("EMB CRS", "Error loading CRS applications: ${e.message}")
                    return@addSnapshotListener
                }

                crsList.clear()

                snapshots?.documents?.forEach { doc ->
                    val data = doc.data ?: return@forEach

                    val applicationId = doc.id
                    val companyName = data["companyName"] as? String ?: "N/A"
                    val companyType = data["companyType"] as? String ?: "N/A"
                    val industryDescriptor = data["industryDescriptor"] as? String ?: "N/A"
                    val natureOfBusiness = data["natureOfBusiness"] as? String ?: "N/A"
                    val status = data["status"] as? String ?: "Pending"
                    val dateSubmitted = data["dateSubmitted"] as? Timestamp ?: Timestamp.now()

                    val crs = CrsApplication(
                        applicationId = applicationId,
                        companyName = companyName,
                        companyType = companyType,
                        industryDescriptor = industryDescriptor,
                        natureOfBusiness = natureOfBusiness,
                        status = status,
                        dateSubmitted = dateSubmitted
                    )

                    crsList.add(crs)
                }

                // Sort newest first
                crsList.sortByDescending { it.dateSubmitted?.seconds }
                applyFilters()
            }
    }


    private fun applyFilters() {
        val safeBinding = _binding ?: return

        val query = safeBinding.etSearch.text.toString().trim().lowercase()
        filteredList.clear()

        crsList.forEach { app ->
            val matchesStatus = selectedStatus == "All" ||
                    app.status.equals(selectedStatus, ignoreCase = true)
            val matchesSearch = query.isEmpty() || listOfNotNull(
                app.companyName,
                app. companyType
            ).any { it.lowercase().contains(query) }

            if (matchesStatus && matchesSearch) filteredList.add(app)
        }

        safeBinding.txtNoApplications.visibility =
            if (filteredList.isEmpty()) View.VISIBLE else View.GONE

        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snapshotListener?.remove()
        snapshotListener = null
        _binding = null
    }
}
