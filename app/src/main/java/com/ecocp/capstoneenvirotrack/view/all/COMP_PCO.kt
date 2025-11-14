package com.ecocp.capstoneenvirotrack.view.all

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.PCO
import com.ecocp.capstoneenvirotrack.view.businesses.adapters.PCOAdapter
import com.ecocp.capstoneenvirotrack.view.businesses.dialogs.PCODetailsDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class COMP_PCO : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoData: TextView
    private lateinit var etSearch: EditText
    private lateinit var spinnerStatus: Spinner
    private lateinit var adapter: PCOAdapter
    private lateinit var backButton: ImageView
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_comp_pco, container, false)

        recyclerView = view.findViewById(R.id.pcoRecyclerView)
        tvNoData = view.findViewById(R.id.tvNoData)
        etSearch = view.findViewById(R.id.etSearch)
        spinnerStatus = view.findViewById(R.id.spinnerStatus)
        backButton = view.findViewById(R.id.backButton)
        val newApplicationButton: FloatingActionButton = view.findViewById(R.id.NewApplication)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        adapter = PCOAdapter(mutableListOf()) { selectedItem ->
            showDetails(selectedItem)
        }
        recyclerView.adapter = adapter

        setupSearch()
        setupSpinner()

        // ✅ Navigation buttons now use NavController directly
        backButton.setOnClickListener {
            findNavController().navigate(R.id.action_COMP_PCO_to_pcoDashboard)
        }

        newApplicationButton.setOnClickListener {
            findNavController().navigate(R.id.action_COMP_PCO_to_COMP_PCOAccreditation)
        }

        fetchAccreditations()
        return view
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter.filter.filter(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupSpinner() {
        val statuses = listOf("All", "Approved", "Rejected", "Pending", "Submitted")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = spinnerAdapter

        spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                adapter.filterByStatus(statuses[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun fetchAccreditations() {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Assuming each accreditation has a field "userId" equal to Firebase UID of the submitter
        firestore.collection("accreditations")
            .whereEqualTo("uid", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                val fetchedList = mutableListOf<PCO>()
                for (doc in documents) {
                    val accreditationId = doc.getString("accreditationId") ?: "N/A"
                    val shortId = if (accreditationId.length >= 4) "ID: ${accreditationId.take(4)}" else "ID: $accreditationId"
                    val fullName = doc.getString("fullName") ?: "N/A"
                    val company = doc.getString("companyAffiliation") ?: "N/A"
                    val status = doc.getString("status") ?: "Submitted"
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val formattedDate = if (timestamp > 0)
                        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
                    else "N/A"

                    fetchedList.add(
                        PCO(
                            appId = shortId,
                            appName = company,
                            applicant = fullName,
                            forwardedTo = "EMB",
                            updatedDate = formattedDate,
                            type = "Accreditation",
                            status = status
                        )
                    )
                }

                if (fetchedList.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    tvNoData.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    tvNoData.visibility = View.GONE
                    adapter.updateList(fetchedList)
                }
            }
            .addOnFailureListener { e ->
                Log.e("COMP_PCO", "Error fetching accreditations", e)
                recyclerView.visibility = View.GONE
                tvNoData.visibility = View.VISIBLE
            }
    }


    private fun showDetails(selectedItem: PCO) {
        firestore.collection("accreditations")
            .whereEqualTo("fullName", selectedItem.applicant)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val doc = docs.documents.first()
                    val dialog = PCODetailsDialog.newInstance(
                        doc.getString("fullName") ?: "N/A",
                        doc.getString("positionDesignation") ?: "N/A",
                        doc.getString("accreditationNumber") ?: "N/A",
                        doc.getString("companyAffiliation") ?: "N/A",
                        doc.getString("educationalBackground") ?: "N/A",
                        doc.getString("experienceInEnvManagement") ?: "N/A",
                        doc.getString("governmentIdUrl"),
                        doc.getString("certificateUrl"),
                        doc.getString("trainingCertificateUrl")
                    )
                    dialog.show(parentFragmentManager, "PCODetailsDialog")
                }
            }
            .addOnFailureListener { e ->
                Log.e("COMP_PCO", "Error fetching details: ${e.message}", e)
            }
    }
}
