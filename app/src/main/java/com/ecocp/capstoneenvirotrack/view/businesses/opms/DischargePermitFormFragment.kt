package com.ecocp.capstoneenvirotrack.view.businesses.opms

import android.app.AlertDialog
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar
import com.ecocp.capstoneenvirotrack.R

class DischargePermitFormFragment : Fragment() {

    private var _binding: FragmentDischargePermitFormBinding? = null
    private val binding get() = _binding!!

    private var fileUris = mutableListOf<Uri>()
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val REQUIRED_FILE_COUNT = 4
    private val MAX_FILE_COUNT = 10

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (!uris.isNullOrEmpty()) {
                fileUris.addAll(uris.distinctBy { it.toString() })

                if (fileUris.size > MAX_FILE_COUNT) {
                    fileUris = fileUris.take(MAX_FILE_COUNT).toMutableList()
                    Toast.makeText(requireContext(), "Limited to $MAX_FILE_COUNT files max.", Toast.LENGTH_SHORT).show()
                }

                val filesList = fileUris.mapIndexed { index, uri -> "${index + 1}. ${uri.lastPathSegment}" }
                    .joinToString("\n")
                binding.txtFileName.text = "Selected ${fileUris.size} file(s):\n$filesList"

                binding.btnSubmitPermit.isEnabled = fileUris.size >= REQUIRED_FILE_COUNT
            }
        }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {

        _binding = FragmentDischargePermitFormBinding.inflate(inflater, container, false)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnSubmitPermit.isEnabled = false

        checkPendingDischargePermit() // Check if user already has a pending application

        binding.btnUploadFile.setOnClickListener {
            val mimeTypes = arrayOf("application/pdf", "image/*")
            filePickerLauncher.launch(mimeTypes)
        }

        binding.inputOperationStartDate.setOnClickListener { showDatePickerDialog() }

        binding.btnSubmitPermit.setOnClickListener { validateAndSubmitForm() }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ✅ Function to check pending discharge permit
    private fun checkPendingDischargePermit() {
        val userUid = auth.currentUser?.uid ?: return
        firestore.collection("opms_discharge_permits")
            .whereEqualTo("uid", userUid)
            .whereEqualTo("status", "Pending")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Pending permit found — show dialog
                    AlertDialog.Builder(requireContext())
                        .setTitle("Pending Application Found")
                        .setMessage("You have a pending Discharge Permit application. Do you want to continue to payment?")
                        .setPositiveButton("Yes") { _, _ ->
                            val doc = querySnapshot.documents.first()
                            val formData = doc.data ?: return@setPositiveButton

                            val bundle = Bundle().apply {
                                putString("documentId", doc.id) // ✅ pass the doc ID
                                putString("companyName", formData["companyName"] as? String)
                                putString("companyAddress", formData["companyAddress"] as? String)
                                putString("pcoName", formData["pcoName"] as? String)
                                putString("pcoAccreditationNumber", formData["pcoAccreditation"] as? String)
                                putString("receivingBody", formData["bodyOfWater"] as? String)
                                putString("dischargeVolume", formData["volume"] as? String)
                                putString("dischargeMethod", formData["treatmentMethod"] as? String)
                                putString("uploadedFiles", formData["fileLinks"] as? String)
                                putString("paymentInfo", "₱1,500 - Pending")
                            }

                            findNavController().navigate(R.id.action_application_to_payment, bundle)
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to check pending application.", Toast.LENGTH_SHORT).show()
            }
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
                "operationStartDate" to operationStartDate,
                "applicationType" to "Discharge Permit"
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
                Toast.makeText(requireContext(), "File upload failed. Check network.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveFormToFirestore(formData: Map<String, String>, fileLinks: List<String>) {
        val userUid = auth.currentUser?.uid ?: return

        val docId = "${userUid}_${System.currentTimeMillis()}" // ✅ One document per submission
        val dataToSave = mutableMapOf<String, Any>()
        dataToSave.putAll(formData)
        dataToSave["fileLinks"] = fileLinks.joinToString(",")
        dataToSave["status"] = "Pending"
        dataToSave["timestamp"] = FieldValue.serverTimestamp()
        dataToSave["uid"] = userUid

        firestore.collection("opms_discharge_permits")
            .document(docId) // ✅ Create with a known ID
            .set(dataToSave)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Discharge Permit submitted successfully!", Toast.LENGTH_LONG).show()

                val bundle = Bundle().apply {
                    putString("documentId", docId) // ✅ pass to payment
                    putString("companyName", formData["companyName"])
                    putString("companyAddress", formData["companyAddress"])
                    putString("pcoName", formData["pcoName"])
                    putString("pcoAccreditationNumber", formData["pcoAccreditation"])
                    putString("receivingBody", formData["bodyOfWater"])
                    putString("dischargeVolume", formData["volume"])
                    putString("dischargeMethod", formData["treatmentMethod"])
                    putString("uploadedFiles", fileLinks.joinToString("\n"))
                    putString("paymentInfo", "₱1,500 - Pending")
                }

                clearForm()
                findNavController().navigate(R.id.action_application_to_payment, bundle)
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
