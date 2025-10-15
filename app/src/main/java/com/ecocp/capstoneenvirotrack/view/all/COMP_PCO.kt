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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.model.PCO
import com.ecocp.capstoneenvirotrack.view.businesses.adapters.PCOAdapter
import com.ecocp.capstoneenvirotrack.view.businesses.dialogs.PCODetailsDialog
import com.ecocp.capstoneenvirotrack.view.businesses.pcoacc.COMP_PCOAccreditation
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
    private val list = mutableListOf<PCO>()
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
        setupButtons(newApplicationButton)
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
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                adapter.filterByStatus(statuses[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupButtons(newApplicationButton: FloatingActionButton) {
        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        newApplicationButton.setOnClickListener {
            val newFragment = COMP_PCOAccreditation()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, newFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun fetchAccreditations() {
        Log.d("COMP_PCO", "Fetching all accreditations...")

        firestore.collection("accreditations")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("COMP_PCO", "Fetched ${documents.size()} documents")
                val fetchedList = mutableListOf<PCO>()

                for (doc in documents) {
                    val accreditationId = doc.getString("accreditationId") ?: "N/A"
                    val shortId = if (accreditationId.length >= 4)
                        "ID: ${accreditationId.take(4)}"
                    else "ID: $accreditationId"

                    val fullName = doc.getString("fullName") ?: "N/A"
                    val company = doc.getString("companyAffiliation") ?: "N/A"
                    val status = doc.getString("status") ?: "Submitted"
                    val timestamp = doc.getLong("timestamp") ?: 0L

                    val formattedDate = if (timestamp > 0)
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
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

                    // Extract the file URLs
                    val governmentIdUrl = doc.getString("governmentIdUrl")
                    val certificateUrl = doc.getString("certificateUrl")
                    val trainingCertificateUrl = doc.getString("trainingCertificateUrl")

                    val dialog = PCODetailsDialog.newInstance(
                        doc.getString("fullName") ?: "N/A",
                        doc.getString("positionDesignation") ?: "N/A",
                        doc.getString("accreditationNumber") ?: "N/A",
                        doc.getString("companyAffiliation") ?: "N/A",
                        doc.getString("educationalBackground") ?: "N/A",
                        doc.getString("experienceInEnvManagement") ?: "N/A",
                        governmentIdUrl,
                        certificateUrl,
                        trainingCertificateUrl
                    )

                    dialog.show(parentFragmentManager, "PCODetailsDialog")
                }
            }
            .addOnFailureListener { e ->
                Log.e("COMP_PCO", "Error fetching details: ${e.message}", e)
            }
    }

}
