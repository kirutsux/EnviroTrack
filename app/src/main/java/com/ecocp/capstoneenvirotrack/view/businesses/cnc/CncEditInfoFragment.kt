package com.ecocp.capstoneenvirotrack.view.businesses.cnc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentCncEditInfoBinding
import com.google.firebase.firestore.FirebaseFirestore

class CncEditInfoFragment : Fragment() {

    private var _binding: FragmentCncEditInfoBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private var applicationId: String? = null

    // List to store uploaded file names (or file references)
    private var uploadedFiles: MutableList<String> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCncEditInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 Get application ID
        applicationId = arguments?.getString("applicationId")
        if (applicationId == null) {
            Toast.makeText(requireContext(), "No application ID found", Toast.LENGTH_SHORT).show()
            return
        }

        // Load existing CNC info
        loadCncInfo()

        // 🔹 Handle Save Changes & Go to Review
        binding.btnReview.setOnClickListener {
            saveChangesAndGoToReview()
        }

        // 🔹 Optional: handle file uploads (example placeholder)
        // You can integrate your file picker here and update uploadedFiles list accordingly.
        // binding.btnUploadFiles.setOnClickListener { pickFiles() }
    }

    private fun saveChangesAndGoToReview() {
        if (applicationId == null) return

        // Gather all inputs
        val updatedData = hashMapOf(
            "companyName" to binding.inputCompanyName.text.toString().trim(),
            "businessName" to binding.inputBusinessName.text.toString().trim(),
            "projectTitle" to binding.inputProjectTitle.text.toString().trim(),
            "natureOfBusiness" to binding.inputNatureOfBusiness.text.toString().trim(),
            "projectLocation" to binding.inputProjectLocation.text.toString().trim(),
            "email" to binding.email.text.toString().trim(),
            "managingHead" to binding.inputManagingHead.text.toString().trim(),
            "pcoName" to binding.inputPcoName.text.toString().trim(),
            "pcoAccreditation" to binding.inputPcoAccreditation.text.toString().trim(),
            "dateEstablished" to binding.inputDateEstablished.text.toString().trim(),
            "numEmployees" to binding.inputEmployees.text.toString().trim(),
            "psicCode" to binding.inputPsicCode.text.toString().trim(),
            "projectType" to binding.inputProjectType.text.toString().trim(),
            "projectScale" to binding.inputProjectScale.text.toString().trim(),
            "projectCost" to binding.inputProjectCost.text.toString().trim(),
            "landArea" to binding.inputLandArea.text.toString().trim(),
            "rawMaterials" to binding.inputRawMaterials.text.toString().trim(),
            "productionCapacity" to binding.inputProductionCapacity.text.toString().trim(),
            "utilitiesUsed" to binding.inputUtilitiesUsed.text.toString().trim(),
            "wasteGenerated" to binding.inputWasteGenerated.text.toString().trim(),
            "coordinates" to binding.inputCoordinates.text.toString().trim(),
            "nearbyWaters" to binding.inputNearbyWaters.text.toString().trim(),
            "residentialProximity" to binding.inputResidentialProximity.text.toString().trim(),
            "envFeatures" to binding.inputEnvFeatures.text.toString().trim(),
            "zoning" to binding.inputZoning.text.toString().trim(),
        )

        // Update Firestore
        firestore.collection("cnc_applications")
            .document(applicationId!!)
            .update(updatedData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "CNC info updated!", Toast.LENGTH_SHORT).show()
                // Navigate to Review Fragment
                val bundle = Bundle().apply {
                    putString("applicationId", applicationId)
                }
                findNavController().navigate(
                    R.id.action_cncEditInfoFragment_to_cncReviewFragment,
                    bundle
                )
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update CNC info.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadCncInfo() {
        firestore.collection("cnc_applications")
            .document(applicationId!!)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {

                    binding.apply {
                        // Fill EditTexts
                        inputCompanyName.setText(doc.getString("companyName"))
                        inputBusinessName.setText(doc.getString("businessName"))
                        inputProjectTitle.setText(doc.getString("projectTitle"))
                        inputNatureOfBusiness.setText(doc.getString("natureOfBusiness"))
                        inputProjectLocation.setText(doc.getString("projectLocation"))
                        email.setText(doc.getString("email"))
                        inputManagingHead.setText(doc.getString("managingHead"))
                        inputPcoName.setText(doc.getString("pcoName"))
                        inputPcoAccreditation.setText(doc.getString("pcoAccreditation"))
                        inputDateEstablished.setText(doc.getString("dateEstablished"))
                        inputEmployees.setText(doc.getString("numEmployees"))
                        inputPsicCode.setText(doc.getString("psicCode"))
                        inputProjectType.setText(doc.getString("projectType"))
                        inputProjectScale.setText(doc.getString("projectScale"))
                        inputProjectCost.setText(doc.getString("projectCost"))
                        inputLandArea.setText(doc.getString("landArea"))
                        inputRawMaterials.setText(doc.getString("rawMaterials"))
                        inputProductionCapacity.setText(doc.getString("productionCapacity"))
                        inputUtilitiesUsed.setText(doc.getString("utilitiesUsed"))
                        inputWasteGenerated.setText(doc.getString("wasteGenerated"))
                        inputCoordinates.setText(doc.getString("coordinates"))
                        inputNearbyWaters.setText(doc.getString("nearbyWaters"))
                        inputResidentialProximity.setText(doc.getString("residentialProximity"))
                        inputEnvFeatures.setText(doc.getString("envFeatures"))
                        inputZoning.setText(doc.getString("zoning"))

                    }

                } else {
                    Toast.makeText(requireContext(), "Document not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load CNC", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
