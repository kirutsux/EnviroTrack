package com.ecocp.capstoneenvirotrack.view.businesses.opms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentOpmsDashboardBinding
import com.google.firebase.firestore.FirebaseFirestore

class OpmsDashboardFragment : Fragment() {

    private var _binding: FragmentOpmsDashboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOpmsDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 Discharge Permit
        binding.btnApplyDischargePermit.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_dischargePermitForm)
        }

        // 🔹 Permit to Operate (PTO)
        binding.btnApplyPto.setOnClickListener {
            Toast.makeText(requireContext(), "PTO application feature coming soon.", Toast.LENGTH_SHORT).show()
            // findNavController().navigate(R.id.action_dashboard_to_ptoForm) // later when you create it
        }

        // 🔹 Solid Waste Management Permit
        binding.btnApplySwm.setOnClickListener {
            Toast.makeText(requireContext(), "Solid Waste Management application feature coming soon.", Toast.LENGTH_SHORT).show()
            // findNavController().navigate(R.id.action_dashboard_to_swmForm) // later when you create it
        }

        // Load existing applications
        loadSubmittedApplications()
    }

    private fun loadSubmittedApplications() {
        db.collection("opms_discharge_permits")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    binding.txtSubmittedList.text = "You have no submitted applications yet."
                } else {
                    val builder = StringBuilder()
                    for (doc in result) {
                        val status = doc.getString("status") ?: "Unknown"
                        builder.append("• Discharge Permit – Status: $status\n")
                    }
                    binding.txtSubmittedList.text = builder.toString()
                }
            }
            .addOnFailureListener {
                binding.txtSubmittedList.text = "Failed to load applications."
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
