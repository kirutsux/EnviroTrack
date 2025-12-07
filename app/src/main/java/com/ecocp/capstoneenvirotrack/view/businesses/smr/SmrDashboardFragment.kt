package com.ecocp.capstoneenvirotrack.view.businesses.smr

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.adapter.SmrListAdapter
import com.ecocp.capstoneenvirotrack.databinding.FragmentSmrDashboardBinding
import com.ecocp.capstoneenvirotrack.model.Smr
import com.ecocp.capstoneenvirotrack.viewmodel.SmrViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SmrDashboardFragment : Fragment(R.layout.fragment_smr_dashboard) {

    private var _binding: FragmentSmrDashboardBinding? = null
    private val binding get() = _binding!!
    private val smrViewModel: SmrViewModel by activityViewModels()
    private lateinit var smrAdapter: SmrListAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val userUid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSmrDashboardBinding.bind(view)

        setupRecyclerView()
        loadSubmittedSmrs()

        // --- Load module progress from ViewModel ---
        smrViewModel.moduleProgress.observe(viewLifecycleOwner) { progressMap ->
            updateModuleProgress(progressMap)
        }

        // --- Module click listeners ---
        binding.cardModule1.setOnClickListener {
            findNavController().navigate(R.id.action_smrDashboardFragment_to_module1GeneralInfoFragment)
        }
        binding.cardModule2.setOnClickListener {
            findNavController().navigate(R.id.action_smrDashboardFragment_to_module2HazardousWasteFragment)
        }
        binding.cardModule3.setOnClickListener {
            findNavController().navigate(R.id.action_smrDashboardFragment_to_module3WaterPollutionFragment)
        }
        binding.cardModule4.setOnClickListener {
            findNavController().navigate(R.id.action_smrDashboardFragment_to_module4AirPollutionFragment)
        }
        binding.cardModule5.setOnClickListener {
            findNavController().navigate(R.id.action_smrDashboardFragment_to_module5OthersFragment)
        }

        // --- Floating button to start new SMR ---
        binding.btnAddSmr.setOnClickListener {
            findNavController().navigate(R.id.action_smrDashboardFragment_to_module1GeneralInfoFragment)
        }

        // --- Back button ---
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onResume(){
        super.onResume()
        loadSubmittedSmrs()
    }

    /** --- Setup RecyclerView for submitted SMRs --- */
    private fun setupRecyclerView() {
        smrAdapter = SmrListAdapter(onItemClick = { smr ->
            findNavController().navigate(
                R.id.action_smrDashboardFragment_to_smrSummaryFragment,
                bundleOf("smrId" to smr.id)
            )
        })
        binding.recyclerSmrList.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSmrList.adapter = smrAdapter
    }

    /** --- Load submitted SMRs from Firestore --- */
    private fun loadSubmittedSmrs() {
        userUid?.let { uid ->
            firestore.collection("smr_submissions")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    val smrList = snapshot.documents.mapNotNull { doc->doc.toObject(Smr::class.java)?.copy(id = doc.id) }
                    android.util.Log.d("Dashboard", "Loaded ${smrList.size} SMRs")
                    if (smrList.isEmpty()) {
                        binding.txtNoApplications.visibility = View.VISIBLE
                    } else {
                        binding.txtNoApplications.visibility = View.GONE
                        smrAdapter.submitList(smrList)
                    }
                }
                .addOnFailureListener {
                    binding.txtNoApplications.visibility = View.VISIBLE
                    binding.txtNoApplications.text = "Failed to load SMRs."
                }
        }
    }

    /** --- Update module progress UI --- */
    private fun updateModuleProgress(progressMap: Map<String, Int>) {
        progressMap["module1"]?.let { binding.progressModule1.progress = it; binding.tvModule1Status.text = "$it%" }
        progressMap["module2"]?.let { binding.progressModule2.progress = it; binding.tvModule2Status.text = "$it%" }
        progressMap["module3"]?.let { binding.progressModule3.progress = it; binding.tvModule3Status.text = "$it%" }
        progressMap["module4"]?.let { binding.progressModule4.progress = it; binding.tvModule4Status.text = "$it%" }
        progressMap["module5"]?.let { binding.progressModule5.progress = it; binding.tvModule5Status.text = "$it%" }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
