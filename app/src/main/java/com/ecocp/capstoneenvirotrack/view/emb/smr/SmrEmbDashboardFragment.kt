package com.ecocp.capstoneenvirotrack.view.emb.smr

import android.annotation.SuppressLint
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
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.SmrEmbAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentSmrEmbDashboardBinding
import com.ecocp.capstoneenvirotrack.model.*
import com.ecocp.capstoneenvirotrack.workers.QuarterlyReminderWorker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.concurrent.TimeUnit

@Suppress("UNCHECKED_CAST")
class SmrEmbDashboardFragment : Fragment() {

    private var _binding: FragmentSmrEmbDashboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private lateinit var adapter: SmrEmbAdapter
    private val smrList = mutableListOf<Smr>()
    private val filteredList = mutableListOf<Smr>()
    private var selectedStatus: String? = ""

    private var snapshotListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSmrEmbDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SmrEmbAdapter(filteredList) { selectedSmr ->
            val bundle = Bundle().apply { putString("submissionId", selectedSmr.id) }
            findNavController().navigate(
                R.id.action_embSmrDashboardFragment_to_embSmrReviewDetailsFragment,
                bundle
            )
        }

        scheduleQuarterlyReminders()

        binding.recyclerSmrList.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSmrList.adapter = adapter

        setupSpinner()
        setupSearchBar()

        loadAllSmrSubmissions()
    }

    private fun scheduleQuarterlyReminders() {
        val workRequest = PeriodicWorkRequestBuilder<QuarterlyReminderWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .setRequiresCharging(false)
                    .build()
            )
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "quarterly_reminder_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }


    private fun setupSpinner() {
        val statusOptions = listOf("All", "Pending", "Reviewed")
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, statusOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = spinnerAdapter

        binding.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
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

    private fun loadAllSmrSubmissions() {
        snapshotListener = db.collection("smr_submissions")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("EMB SMR", "Error loading SMR submissions: ${e.message}")
                    return@addSnapshotListener
                }

                smrList.clear()
                snapshots?.documents?.forEach { doc ->
                    val data = doc.data ?: return@forEach

                    val submittedTimestamp = data["dateSubmitted"] as? Timestamp
                    if (submittedTimestamp != null) {
                        val generalInfoMap = data["generalInfo"] as? Map<*, *>
                        val generalInfo = generalInfoMap?.let {
                            GeneralInfo(
                                establishmentName = it["establishmentName"] as? String ?: "",
                                address = it["address"] as? String ?: "",
                                ownerName = it["ownerName"] as? String ?: "",
                                phone = it["phone"] as? String ?: "",
                                email = it["email"] as? String ?: "",
                                typeOfBusiness = it["typeOfBusiness"] as? String ?: "",
                                ceoName = it["ceoName"] as? String,
                                ceoPhone = it["ceoPhone"] as? String,
                                ceoEmail = it["ceoEmail"] as? String,
                                pcoName = it["pcoName"] as? String ?: "",
                                pcoPhone = it["pcoPhone"] as? String ?: "",
                                pcoEmail = it["pcoEmail"] as? String ?: "",
                                pcoAccreditationNo = it["pcoAccreditationNo"] as? String ?: "",
                                legalClassification = it["legalClassification"] as? String ?: ""
                            )
                        } ?: GeneralInfo()

                        val hazardousWastesList = data["hazardousWastes"] as? List<Map<String, Any>>
                        val hazardousWastes = hazardousWastesList?.map {
                            HazardousWaste(
                                commonName = it["commonName"] as? String ?: "",
                                casNo = it["casNo"] as? String ?: "",
                                tradeName = it["tradeName"] as? String ?: "",
                                hwNo = it["hwNo"] as? String ?: "",
                                hwClass = it["hwClass"] as? String ?: "",
                                hwGenerated = it["hwGenerated"] as? String ?: "",
                                storageMethod = it["storageMethod"] as? String ?: "",
                                transporter = it["transporter"] as? String ?: "",
                                treater = it["treater"] as? String ?: "",
                                disposalMethod = it["disposalMethod"] as? String ?: ""
                            )
                        } ?: emptyList()

                        val status = data["status"] as? String ?: "Pending"

                        val smr = Smr(
                            generalInfo = generalInfo,
                            hazardousWastes = hazardousWastes,
                            submittedAt = submittedTimestamp.toDate().time,
                            uid = data["uid"] as? String,
                            id = doc.id,
                            status = status
                        )
                        smrList.add(smr)
                    }
                }

                smrList.sortByDescending { it.submittedAt }
                applyFilters()
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun applyFilters() {
        val safeBinding = _binding ?: return

        val query = safeBinding.etSearch.text.toString().trim().lowercase()
        filteredList.clear()

        smrList.forEach { smr ->
            val matchesStatus = selectedStatus == "All" || smr.status == selectedStatus
            val matchesSearch = query.isEmpty() || listOfNotNull(
                smr.generalInfo.establishmentName,
                smr.generalInfo.address
            ).any { it.lowercase().contains(query) }

            if (matchesStatus && matchesSearch) {
                filteredList.add(smr)
            }
        }

        if (filteredList.isEmpty()) {
            safeBinding.txtNoSubmissions.visibility = View.VISIBLE
        } else {
            safeBinding.txtNoSubmissions.visibility = View.GONE
        }

        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snapshotListener?.remove()
        snapshotListener = null
        _binding = null
    }
}