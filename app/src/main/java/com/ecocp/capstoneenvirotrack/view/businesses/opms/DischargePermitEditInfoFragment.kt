package com.ecocp.capstoneenvirotrack.view.businesses.opms

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentDischargePermitEditInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

import java.util.Calendar

class DischargePermitEditInfoFragment : Fragment() {

    private var _binding: FragmentDischargePermitEditInfoBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var documentId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDischargePermitEditInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        documentId = arguments?.getString("documentId")
        if (documentId == null) {
            Toast.makeText(requireContext(), "No document found.", Toast.LENGTH_SHORT).show()
            return
        }

        loadPermitData()

        binding.inputOperationStartDate.setOnClickListener { showDatePickerDialog() }

        // 🔹 Handle Save Changes & Go to Review
        binding.btnReview.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadPermitData() {
        firestore.collection("opms_discharge_permits")
            .document(documentId!!)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    binding.apply {
                        inputCompanyName.setText(doc.getString("companyName"))
                        inputCompanyAddress.setText(doc.getString("companyAddress"))
                        inputPcoName.setText(doc.getString("pcoName"))
                        inputPcoAccreditation.setText(doc.getString("pcoAccreditation"))
                        inputContactNumber.setText(doc.getString("contactNumber"))
                        inputEmail.setText(doc.getString("email"))
                        inputBodyOfWater.setText(doc.getString("bodyOfWater"))
                        inputSourceWastewater.setText(doc.getString("sourceWastewater"))
                        inputVolume.setText(doc.getString("volume"))
                        inputTreatmentMethod.setText(doc.getString("treatmentMethod"))
                        inputOperationStartDate.setText(doc.getString("operationStartDate"))
                    }
                } else {
                    Toast.makeText(requireContext(), "Permit data not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load permit data.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val formatted = "$year-${String.format("%02d", month + 1)}-${String.format("%02d", day)}"
                binding.inputOperationStartDate.setText(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveChanges() {
        if (documentId == null) return

        val updatedData = mapOf(
            "companyName" to binding.inputCompanyName.text.toString().trim(),
            "companyAddress" to binding.inputCompanyAddress.text.toString().trim(),
            "pcoName" to binding.inputPcoName.text.toString().trim(),
            "pcoAccreditation" to binding.inputPcoAccreditation.text.toString().trim(),
            "contactNumber" to binding.inputContactNumber.text.toString().trim(),
            "email" to binding.inputEmail.text.toString().trim(),
            "bodyOfWater" to binding.inputBodyOfWater.text.toString().trim(),
            "sourceWastewater" to binding.inputSourceWastewater.text.toString().trim(),
            "volume" to binding.inputVolume.text.toString().trim(),
            "treatmentMethod" to binding.inputTreatmentMethod.text.toString().trim(),
            "operationStartDate" to binding.inputOperationStartDate.text.toString().trim(),
            "timestamp" to FieldValue.serverTimestamp()
        )

        firestore.collection("opms_discharge_permits")
            .document(documentId!!)
            .update(updatedData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Permit updated successfully!", Toast.LENGTH_SHORT).show()
                // Navigate to Review Fragment
                val bundle = Bundle().apply {
                    putString("applicationId", documentId)
                }
                findNavController().navigate(
                    R.id.action_edit_to_review,
                    bundle
                )
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update permit.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
