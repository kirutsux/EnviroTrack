package com.ecocp.capstoneenvirotrack.view.emb.pcoacc

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.PcoEmbAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentPcoEmbDashboardBinding
import com.ecocp.capstoneenvirotrack.model.PcoAccreditation
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class PcoEmbDashboardFragment : Fragment() {

    private var _binding: FragmentPcoEmbDashboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private lateinit var adapter: PcoEmbAdapter
    private val pcoList = mutableListOf<PcoAccreditation>()
    private val filteredList = mutableListOf<PcoAccreditation>()
    private var selectedStatus: String = "All"

    private var snapshotListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPcoEmbDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PcoEmbAdapter(
            filteredList,
            onItemClick = { selectedApp ->
                val bundle = Bundle().apply { putString("applicationId", selectedApp.applicationId) }
                findNavController().navigate(
                    R.id.action_pcoEmbDashboardFragment_to_pcoEmbReviewDetailsFragment,
                    bundle
                )
            },
            onItemLongClick = onItemLongClick@{ pco ->
                // ✅ Only allow delete for pending or rejected applications
                if (!pco.status.equals("Pending", ignoreCase = true) &&
                    !pco.status.equals("Rejected", ignoreCase = true)
                ) {
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Cannot Delete")
                        .setMessage("Only pending or rejected applications can be deleted.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@onItemLongClick
                }

                // ✅ Confirm deletion dialog
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Application")
                    .setMessage("Are you sure you want to delete this PCO Accreditation application?")
                    .setPositiveButton("Delete") { _, _ ->
                        val appId = pco.applicationId ?: return@setPositiveButton

                        db.collection("accreditations").document(appId)
                            .delete()
                            .addOnSuccessListener {
                                // Update the list and refresh adapter
                                val updatedList = pcoList.toMutableList()
                                updatedList.remove(pco)
                                pcoList.clear()
                                pcoList.addAll(updatedList)
                                applyFilters()

                                Toast.makeText(
                                    requireContext(),
                                    "Application deleted successfully.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to delete application: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.recyclerEmbPcoList.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerEmbPcoList.adapter = adapter

        setupSpinner()
        setupSearchBar()
        loadAllPcoApplications()
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

    private fun loadAllPcoApplications() {
        snapshotListener = db.collection("accreditations")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("EMB PCO", "Error loading PCO Accreditation apps: ${e.message}")
                    return@addSnapshotListener
                }

                pcoList.clear()

                snapshots?.documents?.forEach { doc ->
                    val data = doc.data ?: return@forEach

                    val submittedTimestamp =
                        (data["submittedTimestamp"] as? Timestamp) ?: Timestamp.now()
                    val issueDate = data["issueDate"] as? Timestamp
                    val expiryDate = data["expiryDate"] as? Timestamp

                    val applicationId = doc.id
                    val pcoName = data["fullName"] as? String ?: "N/A"
                    val companyAffiliation = data["companyAffiliation"] as? String ?: "N/A"
                    val positionDesignation = data["positionDesignation"] as? String ?: "N/A"
                    val status = data["status"] as? String ?: "Pending"


                    val pco = PcoAccreditation(
                        applicationId = applicationId,
                        fullName = pcoName,
                        companyAffiliation = companyAffiliation,
                        positionDesignation = positionDesignation,
                        status = status,
                        submittedTimestamp= submittedTimestamp,
                        issueDate = issueDate,
                        expiryDate = expiryDate
                    )

                    pcoList.add(pco)
                }

                // Sort newest first (based on string date fallback)
                pcoList.reverse()
                applyFilters()
            }
    }

    private fun applyFilters() {
        val safeBinding = _binding ?: return

        val query = safeBinding.etSearch.text.toString().trim().lowercase()
        filteredList.clear()

        pcoList.forEach { app ->
            val matchesStatus = selectedStatus == "All" ||
                    app.status.equals(selectedStatus, ignoreCase = true)
            val matchesSearch = query.isEmpty() || listOfNotNull(
                app.companyAffiliation,
                app.fullName
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
