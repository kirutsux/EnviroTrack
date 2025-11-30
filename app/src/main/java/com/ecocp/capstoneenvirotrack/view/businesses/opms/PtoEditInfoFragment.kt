package com.ecocp.capstoneenvirotrack.view.businesses.opms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentPtoEditInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PtoEditInfoFragment : Fragment() {

    private var _binding: FragmentPtoEditInfoBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var applicationId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPtoEditInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applicationId = arguments?.getString("applicationId")
        if (applicationId == null) {
            Toast.makeText(requireContext(), "No application found.", Toast.LENGTH_SHORT).show()
            return
        }

        loadPtoData()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // 🔹 Handle Save Changes & Go to Review
        binding.btnReview.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadPtoData() {
        firestore.collection("opms_pto_applications")
            .document(applicationId!!)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    binding.apply {
                        inputOwnerName.setText(doc.getString("ownerName"))
                        inputEstablishmentName.setText(doc.getString("establishmentName"))
                        inputMailingAddress.setText(doc.getString("mailingAddress"))
                        inputPlantAddress.setText(doc.getString("plantAddress"))
                        inputTin.setText(doc.getString("tin"))
                        inputOwnershipType.setText(doc.getString("ownershipType"))
                        inputNatureOfBusiness.setText(doc.getString("natureOfBusiness"))
                        inputPcoName.setText(doc.getString("pcoName"))
                        inputPcoAccreditation.setText(doc.getString("pcoAccreditation"))
                        inputOperatingHours.setText(doc.getString("operatingHours"))
                        inputTotalEmployees.setText(doc.getString("totalEmployees"))
                        inputLandArea.setText(doc.getString("landArea"))
                        inputEquipmentName.setText(doc.getString("equipmentName"))
                        inputFuelType.setText(doc.getString("fuelType"))
                        inputEmissions.setText(doc.getString("emissionsSummary"))
                    }
                } else {
                    Toast.makeText(requireContext(), "PTO application not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load PTO data.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveChanges() {
        if (applicationId == null) return

        val updatedData = mapOf(
            "ownerName" to binding.inputOwnerName.text.toString().trim(),
            "establishmentName" to binding.inputEstablishmentName.text.toString().trim(),
            "mailingAddress" to binding.inputMailingAddress.text.toString().trim(),
            "plantAddress" to binding.inputPlantAddress.text.toString().trim(),
            "tin" to binding.inputTin.text.toString().trim(),
            "ownershipType" to binding.inputOwnershipType.text.toString().trim(),
            "natureOfBusiness" to binding.inputNatureOfBusiness.text.toString().trim(),
            "pcoName" to binding.inputPcoName.text.toString().trim(),
            "pcoAccreditation" to binding.inputPcoAccreditation.text.toString().trim(),
            "operatingHours" to binding.inputOperatingHours.text.toString().trim(),
            "totalEmployees" to binding.inputTotalEmployees.text.toString().trim(),
            "landArea" to binding.inputLandArea.text.toString().trim(),
            "equipmentName" to binding.inputEquipmentName.text.toString().trim(),
            "fuelType" to binding.inputFuelType.text.toString().trim(),
            "emissionsSummary" to binding.inputEmissions.text.toString().trim(),
            "lastEditedTimestamp" to FieldValue.serverTimestamp()
        )

        firestore.collection("opms_pto_applications")
            .document(applicationId!!)
            .update(updatedData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "PTO updated successfully!", Toast.LENGTH_SHORT).show()
                // Navigate back to PTO Review
                val bundle = Bundle().apply {
                    putString("applicationId", applicationId)
                }
                findNavController().navigate(R.id.action_ptoEditInfoFragment_to_ptoReviewFragment, bundle)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update PTO application.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
