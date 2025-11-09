package com.ecocp.capstoneenvirotrack.view.businesses.cnc

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentCncFormBinding
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class CncFormFragment : Fragment() {

    private var _binding: FragmentCncFormBinding? = null
    private val binding get() = _binding!!

    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val REQUIRED_FILE_COUNT = 6
    private val MAX_FILE_COUNT = 10
    private var fileUris = mutableListOf<Uri>()

    private val filePickerLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (!uris.isNullOrEmpty()) {
                fileUris.addAll(uris.distinctBy { it.toString() })

                if (fileUris.size > MAX_FILE_COUNT) {
                    fileUris = fileUris.take(MAX_FILE_COUNT).toMutableList()
                    Toast.makeText(requireContext(), "Limited to $MAX_FILE_COUNT files max.", Toast.LENGTH_SHORT).show()
                }

                val filesList = fileUris.mapIndexed { index, uri -> "${index + 1}. ${uri.lastPathSegment}" }
                    .joinToString("\n")
                binding.txtFileNames.text = "Selected ${fileUris.size} file(s):\n$filesList"

                updateSubmitButtonState()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentCncFormBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // Initial button state
        updateSubmitButtonState()

        // 📅 Date Picker
        binding.inputDateEstablished.setOnClickListener { showDatePickerDialog(binding.inputDateEstablished) }

        // 📂 Upload Files
        binding.btnUploadFiles.setOnClickListener {
            val mimeTypes = arrayOf("application/pdf", "image/*")
            filePickerLauncher.launch(mimeTypes)
        }

        binding.btnSubmitCnc.setOnClickListener { validateAndSubmitForm() }
    }

    private fun updateSubmitButtonState() {
        val enabled = fileUris.size >= REQUIRED_FILE_COUNT
        binding.btnSubmitCnc.isEnabled = enabled
        val color = if (enabled) R.color.purple_500 else R.color.gray // replace with your colors
        binding.btnSubmitCnc.setBackgroundColor(ContextCompat.getColor(requireContext(), color))
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth -> editText.setText(String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)) },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun validateAndSubmitForm() {
        val formData = mapOf(
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
            "zoning" to binding.inputZoning.text.toString().trim()
        )

        if (formData.values.any { it.isEmpty() }) {
            Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (fileUris.size < REQUIRED_FILE_COUNT) {
            Toast.makeText(requireContext(), "Please upload all required documents", Toast.LENGTH_SHORT).show()
            return
        }

        uploadFilesToFirebase(formData)
    }

    private fun uploadFilesToFirebase(formData: Map<String, String>) {
        val userUid = auth.currentUser?.uid ?: return
        Toast.makeText(requireContext(), "Uploading files, please wait...", Toast.LENGTH_SHORT).show()

        val uploadTasks = fileUris.map { uri ->
            val ref = storage.reference.child("cnc_applications/$userUid/${System.currentTimeMillis()}_${uri.lastPathSegment}")
            ref.putFile(uri).continueWithTask { ref.downloadUrl }
        }

        Tasks.whenAllSuccess<Uri>(uploadTasks)
            .addOnSuccessListener { urls ->
                val fileLinks = urls.map { it.toString() }
                saveFormToFirestore(formData, fileLinks)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "File upload failed. Check network.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveFormToFirestore(formData: Map<String, String>, fileLinks: List<String>) {
        val userUid = auth.currentUser?.uid ?: return

        val docId = "${userUid}_${System.currentTimeMillis()}"
        val dataToSave = mutableMapOf<String, Any>()
        dataToSave.putAll(formData)
        dataToSave["fileLinks"] = fileLinks.joinToString(",")
        dataToSave["status"] = "Pending"
        dataToSave["amount"] = 50
        dataToSave["currency"] = "PHP"
        dataToSave["applicationType"] = "Certificate of Non-Coverage (CNC)"
        dataToSave["paymentStatus"] = "Unpaid"
        dataToSave["submittedTimestamp"] = Timestamp.now()
        dataToSave["timestamp"] = FieldValue.serverTimestamp()
        dataToSave["uid"] = userUid

        firestore.collection("cnc_applications")
            .document(docId)
            .set(dataToSave)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "CNC Application submitted!", Toast.LENGTH_LONG).show()

                val bundle = Bundle().apply {
                    putString("applicationId", docId)
                    putString("uploadedFiles", fileLinks.joinToString("\n"))
                }

                clearForm()
                findNavController().navigate(R.id.action_cncFormFragment_to_cncPaymentFragment, bundle)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error submitting CNC application", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearForm() {
        binding.apply {
            inputCompanyName.text?.clear()
            inputBusinessName.text?.clear()
            inputProjectTitle.text?.clear()
            inputNatureOfBusiness.text?.clear()
            inputProjectLocation.text?.clear()
            email.text?.clear()
            inputManagingHead.text?.clear()
            inputPcoName.text?.clear()
            inputPcoAccreditation.text?.clear()
            inputDateEstablished.text?.clear()
            inputEmployees.text?.clear()
            inputPsicCode.text?.clear()
            inputProjectType.text?.clear()
            inputProjectScale.text?.clear()
            inputProjectCost.text?.clear()
            inputLandArea.text?.clear()
            inputRawMaterials.text?.clear()
            inputProductionCapacity.text?.clear()
            inputUtilitiesUsed.text?.clear()
            inputWasteGenerated.text?.clear()
            inputCoordinates.text?.clear()
            inputNearbyWaters.text?.clear()
            inputResidentialProximity.text?.clear()
            inputEnvFeatures.text?.clear()
            inputZoning.text?.clear()
            txtFileNames.text = "No files selected"
            fileUris.clear()
            updateSubmitButtonState()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
