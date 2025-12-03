package com.ecocp.capstoneenvirotrack.view.emb.hwms

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.PttApplicationAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentHwmsEmbDashboardBinding
import com.ecocp.capstoneenvirotrack.model.EmbPttApplication
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

class HwmsEmbDashboardFragment : Fragment() {

    private var _binding: FragmentHwmsEmbDashboardBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: PttApplicationAdapter

    private val appList = mutableListOf<EmbPttApplication>()
    private val filteredList = mutableListOf<EmbPttApplication>()
    private var selectedStatus: String = "All"

    private var snapshotListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHwmsEmbDashboardBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PttApplicationAdapter(filteredList) { selectedApp ->
            // Log click for debugging
            Log.d("HWMS", "PTT clicked: ${selectedApp.pttId}, Generator: ${selectedApp.generatorName}")

            // Pass the ID via bundle (no safe args)
            val bundle = Bundle().apply { putString("pttId", selectedApp.pttId) }
            findNavController().navigate(
                R.id.action_hwmsEmbDashboardFragment_to_hwmsEmbReviewDetailsFragment,
                bundle
            )
        }

        binding.recyclerEmbHwmsList.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerEmbHwmsList.adapter = adapter

        setupSpinner()
        setupSearchBar()
        loadAllPttApplications()
    }

    private fun setupSpinner() {
        val statuses = listOf("All", "Pending Review", "Approved", "Rejected")
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, statuses)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = spinnerAdapter

        binding.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedStatus = statuses[position]
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

    private fun loadAllPttApplications() {
        snapshotListener = db.collection("ptt_applications")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("HWMS", "Error fetching PTT applications: ${e.message}")
                    return@addSnapshotListener
                }

                appList.clear()
                snapshots?.documents?.forEach { doc ->
                    val app = doc.toObject(EmbPttApplication::class.java)
                    app?.let {
                        it.pttId = doc.id
                        if (it.generatorName.isNullOrEmpty()) {
                            it.generatorName = "Unknown"
                        }
                        appList.add(it)
                    }
                }

                Log.d("HWMS", "Fetched ${appList.size} PTT applications")
                appList.sortByDescending { it.submittedAtDate() }
                applyFilters()
            }
    }

    private fun applyFilters() {
        filteredList.clear()
        val query = binding.etSearch.text.toString().trim().lowercase(Locale.getDefault())

        appList.forEach { app ->
            val matchesStatus = selectedStatus == "All" ||
                    app.status.equals(selectedStatus, ignoreCase = true)

            val matchesSearch = query.isEmpty() || listOfNotNull(
                app.generatorName,
                app.paymentStatus,
                app.remarks
            ).any { it.lowercase(Locale.getDefault()).contains(query) }

            if (matchesStatus && matchesSearch) filteredList.add(app)
        }

        binding.txtNoApplications.visibility =
            if (filteredList.isEmpty()) View.VISIBLE else View.GONE

        Log.d("HWMS", "Filtered list size: ${filteredList.size}")
        adapter.updateList(filteredList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snapshotListener?.remove()
        snapshotListener = null
        _binding = null
    }
}
