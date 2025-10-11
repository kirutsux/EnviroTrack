package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.PcoHwmsGeneratorDashboardBinding
import com.ecocp.capstoneenvirotrack.view.businesses.adapters.GeneratorDashboardAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class GeneratorDashboardFragment : Fragment() {

    private var _binding: PcoHwmsGeneratorDashboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: GeneratorDashboardAdapter
    private val applications = mutableListOf<GeneratorApplication>()
    private val filteredList = mutableListOf<GeneratorApplication>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PcoHwmsGeneratorDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = GeneratorDashboardAdapter(filteredList)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // Swipe-to-refresh
        binding.swipeRefresh.setOnRefreshListener { fetchApplications() }

        // Add new generator application
        binding.btnAddApplication.setOnClickListener {
            navigateToFragment(GeneratorApplicationFragment())
        }

        // Back button
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Spinner setup
        val statusOptions = arrayOf("All", "Pending", "Approved", "Rejected")
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            statusOptions
        )
        binding.spinnerStatus.adapter = spinnerAdapter

        binding.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                filterApplications()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Search filter
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filterApplications()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Initial load
        fetchApplications()
    }

    private fun fetchApplications() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.swipeRefresh.isRefreshing = true

        db.collection("HazardousWasteGenerator")
            .whereEqualTo("userId", uid)
            .orderBy("dateSubmitted", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                applications.clear()

                for (doc in snapshot.documents) {
                    val timestamp = doc.get("dateSubmitted")
                    val formattedDate = when (timestamp) {
                        is Timestamp -> SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(timestamp.toDate())
                        is Date -> SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(timestamp)
                        is String -> timestamp
                        else -> "N/A"
                    }

                    val app = GeneratorApplication(
                        id = doc.id,
                        companyName = doc.getString("companyName") ?: "N/A",
                        establishmentName = doc.getString("establishmentName") ?: "N/A",
                        pcoName = doc.getString("pcoName") ?: "N/A",
                        natureOfBusiness = doc.getString("natureOfBusiness") ?: "N/A",
                        status = doc.getString("status") ?: "Pending",
                        dateSubmitted = formattedDate
                    )

                    applications.add(app)
                }

                filterApplications()
                binding.swipeRefresh.isRefreshing = false
            }
            .addOnFailureListener { e ->
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ✅ Enhanced Search & Filter
    private fun filterApplications() {
        val searchText = binding.searchBar.text.toString().trim().lowercase(Locale.getDefault())
        val selectedStatus = binding.spinnerStatus.selectedItem.toString()

        filteredList.clear()

        for (app in applications) {
            val matchesSearch = listOf(
                app.companyName,
                app.establishmentName,
                app.pcoName,
                app.natureOfBusiness,
                app.status,
                app.dateSubmitted
            ).any { it.lowercase(Locale.getDefault()).contains(searchText) }

            val matchesStatus =
                selectedStatus == "All" || app.status.equals(selectedStatus, ignoreCase = true)

            if (matchesSearch && matchesStatus) {
                filteredList.add(app)
            }
        }

        adapter.notifyDataSetChanged()
    }

    private fun navigateToFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.nav_host_fragment, fragment)
            addToBackStack(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ✅ Data Model
data class GeneratorApplication(
    val id: String,
    val companyName: String,
    val establishmentName: String,
    val pcoName: String,
    val natureOfBusiness: String,
    val status: String,
    val dateSubmitted: String
)
