package com.ecocp.capstoneenvirotrack.view.emb.opms

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
import com.ecocp.capstoneenvirotrack.adapter.OpmsEmbAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentOpmsEmbDashboardBinding
import com.ecocp.capstoneenvirotrack.model.EmbOpmsApplication
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class OpmsEmbDashboardFragment : Fragment() {

    private var _binding: FragmentOpmsEmbDashboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var adapter: OpmsEmbAdapter
    private val opmsList = mutableListOf<EmbOpmsApplication>()
    private val filteredList = mutableListOf<EmbOpmsApplication>()
    private var selectedStatus: String = "All"

    private var snapshotListeners: MutableList<ListenerRegistration> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOpmsEmbDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = OpmsEmbAdapter(
            filteredList,
            onItemLongClick = onItemLongClick@{ app ->
                val status = app.status ?: "Pending"

                // 🚫 Prevent deleting approved applications
                if (status.equals("Approved", ignoreCase = true)) {
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Cannot Delete")
                        .setMessage("Approved applications cannot be deleted.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@onItemLongClick
                }

                // ✅ Only allow deletion for Pending or Rejected
                if (!status.equals("Pending", ignoreCase = true) &&
                    !status.equals("Rejected", ignoreCase = true)) {
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Cannot Delete")
                        .setMessage("Only pending or rejected applications can be deleted.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@onItemLongClick
                }

                // 🗑️ Confirm deletion dialog
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Application")
                    .setMessage("Are you sure you want to delete this submitted application?")
                    .setPositiveButton("Delete") { _, _ ->
                        val appId = app.applicationId ?: return@setPositiveButton
                        val collection = when (app.applicationType) {
                            "Discharge Permit" -> "opms_discharge_permits"
                            "Permit to Operate" -> "opms_pto_applications"
                            else -> return@setPositiveButton
                        }

                        db.collection(collection).document(appId)
                            .delete()
                            .addOnSuccessListener {
                                val updatedList = filteredList.toMutableList()
                                updatedList.remove(app)
                                filteredList.clear()
                                filteredList.addAll(updatedList)
                                adapter.notifyDataSetChanged()

                                android.widget.Toast.makeText(
                                    requireContext(),
                                    "Application deleted successfully.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                android.widget.Toast.makeText(
                                    requireContext(),
                                    "Failed to delete application: ${e.message}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.recyclerEmbOpmsList.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerEmbOpmsList.adapter = adapter

        setupSpinner()
        setupSearchBar()
        loadApplications()
    }


    private fun setupSpinner() {
        val statusOptions = listOf("All", "Pending", "Approved", "Rejected")
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, statusOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = spinnerAdapter

        binding.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedStatus = statusOptions[position]
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupSearchBar() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { applyFilters() }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadApplications() {
        val uid = auth.currentUser?.uid ?: return
        opmsList.clear()

        // Load Discharge Permit applications
        val dpListener = db.collection("opms_discharge_permits")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("EMB OPMS", "Error loading discharge permits: ${e.message}")
                    return@addSnapshotListener
                }
                snapshot?.documents?.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val app = EmbOpmsApplication(
                        applicationId = doc.id,
                        applicationType = "Discharge Permit",
                        companyName = data["companyName"] as? String,
                        companyAddress = data["companyAddress"] as? String,
                        pcoName = data["pcoName"] as? String,
                        pcoAccreditationNumber = data["pcoAccreditation"] as? String,
                        receivingBody = data["bodyOfWater"] as? String,
                        dischargeVolume = data["volume"] as? String,
                        dischargeMethod = data["treatmentMethod"] as? String,
                        uploadedFiles = data["fileLinks"] as? String,
                        status = data["status"] as? String ?: "Pending",
                        submittedTimestamp = data["submittedTimestamp"] as? Timestamp,
                        issueDate = data["issueDate"] as? Timestamp,
                        expiryDate = data["expiryDate"] as? Timestamp
                    )
                    addOrUpdateApplication(app)
                }
                applyFilters()
            }
        snapshotListeners.add(dpListener)

        // Load Permit to Operate applications
        val ptoListener = db.collection("opms_pto_applications")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("EMB OPMS", "Error loading PTO apps: ${e.message}")
                    return@addSnapshotListener
                }
                snapshot?.documents?.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val app = EmbOpmsApplication(
                        applicationId = doc.id,
                        applicationType = "Permit to Operate",
                        ownerName = data["ownerName"] as? String,
                        establishmentName = data["establishmentName"] as? String,
                        mailingAddress = data["mailingAddress"] as? String,
                        plantAddress = data["plantAddress"] as? String,
                        tin = data["tin"] as? String,
                        ownershipType = data["ownershipType"] as? String,
                        natureOfBusiness = data["natureOfBusiness"] as? String,
                        pcoName = data["pcoName"] as? String,
                        pcoAccreditation = data["pcoAccreditation"] as? String,
                        operatingHours = data["operatingHours"] as? String,
                        totalEmployees = data["totalEmployees"] as? String,
                        landArea = data["landArea"] as? String,
                        equipmentName = data["equipmentName"] as? String,
                        fuelType = data["fuelType"] as? String,
                        emissions = data["emissions"] as? String,
                        status = data["status"] as? String ?: "Pending",
                        submittedTimestamp = data["submittedTimestamp"] as? Timestamp,
                        issueDate = data["issueDate"] as? Timestamp,
                        expiryDate = data["expiryDate"] as? Timestamp
                    )
                    addOrUpdateApplication(app)
                }
                applyFilters()
            }
        snapshotListeners.add(ptoListener)
    }

    private fun addOrUpdateApplication(app: EmbOpmsApplication) {
        val index = opmsList.indexOfFirst { it.applicationId == app.applicationId }
        if (index >= 0) {
            opmsList[index] = app
        } else {
            opmsList.add(app)
        }
        opmsList.sortByDescending { it.submittedTimestamp?.toDate() }
    }

    private fun applyFilters() {
        val safeBinding = _binding ?: return
        val query = safeBinding.etSearch.text.toString().trim().lowercase()
        filteredList.clear()

        opmsList.forEach { app ->
            val matchesStatus = selectedStatus == "All" || app.status.equals(selectedStatus, ignoreCase = true)
            val matchesSearch = query.isEmpty() || listOfNotNull(
                app.applicationType, app.companyName, app.establishmentName, app.plantAddress, app.dischargeMethod
            ).any { it.lowercase().contains(query) }

            if (matchesStatus && matchesSearch) filteredList.add(app)
        }

        safeBinding.txtNoApplications.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snapshotListeners.forEach { it.remove() }
        snapshotListeners.clear()
        _binding = null
    }
}
