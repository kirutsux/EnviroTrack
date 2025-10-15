package com.ecocp.capstoneenvirotrack.view.businesses.opms

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.databinding.FragmentDischargePermitFormBinding
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar
import com.ecocp.capstoneenvirotrack.R

class DischargePermitFormFragment : Fragment() {

    private lateinit var binding: FragmentDischargePermitFormBinding
    private var fileUris = mutableListOf<Uri>()
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val REQUIRED_FILE_COUNT = 4 // Adjust based on required documents
    private val MAX_FILE_COUNT = 10 // Optional: max files

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (!uris.isNullOrEmpty()) {
            fileUris.addAll(uris)

            // Remove duplicates
            val uniqueUris = fileUris.distinctBy { it.toString() }
            fileUris.clear()
            fileUris.addAll(uniqueUris)

            // Limit to max files
            if (fileUris.size > MAX_FILE_COUNT) {
                fileUris.clear()
                fileUris.addAll(uniqueUris.take(MAX_FILE_COUNT))
                Toast.makeText(requireContext(), "Limited to $MAX_FILE_COUNT files max.", Toast.LENGTH_SHORT).show()
            }

            val filesList = fileUris.mapIndexed { index, uri -> "${index + 1}. ${uri.lastPathSegment}" }.joinToString("\n")
            binding.txtFileName.text = "Selected ${fileUris.size} file(s):\n$filesList"

            // Enable submit button only when required files uploaded
            binding.btnSubmitPermit.isEnabled = fileUris.size >= REQUIRED_FILE_COUNT
        }
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        binding = FragmentDischargePermitFormBinding.inflate(inflater, container, false)

        binding.btnSubmitPermit.isEnabled = false

        // File upload button
        binding.btnUploadFile.setOnClickListener {
            val mimeTypes = arrayOf("application/pdf", "image/*")
            filePickerLauncher.launch(mimeTypes)
        }

        // Date picker
        binding.inputOperationStartDate.setOnClickListener { showDatePickerDialog() }

        // Submit button
        binding.btnSubmitPermit.setOnClickListener { validateAndSubmitForm() }

        return binding.root
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val formatted = "$y-${String.format("%02d", m + 1)}-${String.format("%02d", d)}"
                binding.inputOperationStartDate.setText(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun validateAndSubmitForm() {
        val companyName = binding.inputCompanyName.text.toString().trim()
        val companyAddress = binding.inputCompanyAddress.text.toString().trim()
        val pcoName = binding.inputPcoName.text.toString().trim()
        val pcoAccreditation = binding.inputPcoAccreditation.text.toString().trim()
        val contactNumber = binding.inputContactNumber.text.toString().trim()
        val email = binding.inputEmail.text.toString().trim()
        val bodyOfWater = binding.inputBodyOfWater.text.toString().trim()
        val sourceWastewater = binding.inputSourceWastewater.text.toString().trim()
        val volume = binding.inputVolume.text.toString().trim()
        val treatmentMethod = binding.inputTreatmentMethod.text.toString().trim()
        val operationStartDate = binding.inputOperationStartDate.text.toString().trim()

        if (companyName.isEmpty() || companyAddress.isEmpty() || pcoName.isEmpty() ||
            pcoAccreditation.isEmpty() || contactNumber.isEmpty() || email.isEmpty() ||
            bodyOfWater.isEmpty() || sourceWastewater.isEmpty() ||
            volume.isEmpty() || treatmentMethod.isEmpty() || operationStartDate.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (fileUris.size < REQUIRED_FILE_COUNT) {
            Toast.makeText(requireContext(), "Please upload all required documents", Toast.LENGTH_SHORT).show()
            return
        }

        uploadFilesToFirebase(
            mapOf(
                "companyName" to companyName,
                "companyAddress" to companyAddress,
                "pcoName" to pcoName,
                "pcoAccreditation" to pcoAccreditation,
                "contactNumber" to contactNumber,
                "email" to email,
                "bodyOfWater" to bodyOfWater,
                "sourceWastewater" to sourceWastewater,
                "volume" to volume,
                "treatmentMethod" to treatmentMethod,
                "operationStartDate" to operationStartDate
            )
        )
    }

    private fun uploadFilesToFirebase(formData: Map<String, String>) {
        val userUid = auth.currentUser?.uid ?: return
        Toast.makeText(requireContext(), "Uploading files, please wait...", Toast.LENGTH_SHORT).show()

        val uploadTasks = fileUris.map { uri ->
            val ref = storage.reference.child("opms_discharge_permits/$userUid/${System.currentTimeMillis()}_${uri.lastPathSegment}")
            ref.putFile(uri).continueWithTask { ref.downloadUrl }
        }

        Tasks.whenAllSuccess<Uri>(uploadTasks)
            .addOnSuccessListener { urls ->
                val fileLinks = urls.map { it.toString() }
                saveFormToFirestore(formData, fileLinks)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "File upload failed. Check permissions/network.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveFormToFirestore(formData: Map<String, String>, fileLinks: List<String>) {
        val dataToSave = formData.toMutableMap()
        dataToSave["fileLinks"] = fileLinks.joinToString(",")
        dataToSave["status"] = "Pending"
        dataToSave["timestamp"] = Timestamp.now().toString()

        firestore.collection("opms_discharge_permits")
            .add(dataToSave)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Discharge Permit submitted successfully!", Toast.LENGTH_LONG).show()
                clearForm()
                // ✅ Navigate to Payment Fragment
                findNavController().navigate(R.id.action_application_to_payment)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error submitting permit. Try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearForm() {
        binding.inputCompanyName.text?.clear()
        binding.inputCompanyAddress.text?.clear()
        binding.inputPcoName.text?.clear()
        binding.inputPcoAccreditation.text?.clear()
        binding.inputContactNumber.text?.clear()
        binding.inputEmail.text?.clear()
        binding.inputBodyOfWater.text?.clear()
        binding.inputSourceWastewater.text?.clear()
        binding.inputVolume.text?.clear()
        binding.inputTreatmentMethod.text?.clear()
        binding.inputOperationStartDate.text?.clear()
        binding.txtFileName.text = "No files selected"
        fileUris.clear()
        binding.btnSubmitPermit.isEnabled = false
    }
}
