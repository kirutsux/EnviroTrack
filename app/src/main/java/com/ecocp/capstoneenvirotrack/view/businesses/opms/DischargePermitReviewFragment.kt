package com.ecocp.capstoneenvirotrack.view.businesses.opms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentDischargePermitReviewBinding
import com.google.firebase.firestore.FirebaseFirestore

class DischargePermitReviewFragment : Fragment() {

    private var _binding: FragmentDischargePermitReviewBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDischargePermitReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Edit info (go back to form)
        binding.btnEditInfo.setOnClickListener {
            findNavController().popBackStack(R.id.dischargePermitFormFragment, false)
        }

        // Submit final application
        binding.btnSubmitApplication.setOnClickListener {
            db.collection("opms_discharge_permits")
                .add(mapOf("status" to "Submitted"))
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Application submitted successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.opmsDashboardFragment)
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to submit application.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
