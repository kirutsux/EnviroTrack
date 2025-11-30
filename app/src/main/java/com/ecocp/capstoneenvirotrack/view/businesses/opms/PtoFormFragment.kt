package com.ecocp.capstoneenvirotrack.view.businesses.opms

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentPtoFormBinding
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class PtoFormFragment : Fragment() {

    private var _binding: FragmentPtoFormBinding? = null
    private val binding get() = _binding!!

    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var fileUris = mutableListOf<Uri>()
    private val REQUIRED_FILE_COUNT = 1
    private val MAX_FILE_COUNT = 12

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (!uris.isNullOrEmpty()) {
                fileUris.addAll(uris.distinctBy { it.toString() })
                if (fileUris.size > MAX_FILE_COUNT) {
                    fileUris = fileUris.take(MAX_FILE_COUNT).toMutableList()
                    Toast.makeText(requireContext(), "Limited to $MAX_FILE_COUNT files max.", Toast.LENGTH_SHORT).show()
                }
                val filesList = fileUris.mapIndexed { idx, uri -> "${idx + 1}. ${uri.lastPathSegment}" }
                    .joinToString("\n")
                binding.txtFileName.text = "Selected ${fileUris.size} file(s):\n$filesList"
                binding.btnSubmitPto.isEnabled = fileUris.size >= REQUIRED_FILE_COUNT
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPtoFormBinding.inflate(inflater, container, false)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnUploadFile.setOnClickListener {
            val mime = arrayOf("application/pdf", "image/*")
            filePickerLauncher.launch(mime)
        }

        binding.btnSubmitPto.isEnabled = false
        binding.btnSubmitPto.setOnClickListener { validateAndSave() }

        checkPendingPto()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkPendingPto() {
        val userUid = auth.currentUser?.uid ?: return

        firestore.collection("opms_pto_applications")
            .whereEqualTo("uid", userUid)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents.first()
                    val status = doc.getString("status") ?: "Pending"
                    val paymentStatus = doc.getString("paymentStatus") ?: "Pending"
                    val submittedTimestamp = doc.getTimestamp("submittedTimestamp") // null if not submitted
                    val formData = doc.data ?: emptyMap<String, Any>()

                    when {
                        // Pending & payment not done → continue to payment
                        status.equals("Pending", true) && paymentStatus.equals("Pending", true) -> {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Pending PTO Found")
                                .setMessage("You have a pending PTO application. Do you want to continue to payment?")
                                .setPositiveButton("Yes") { _, _ ->
                                    val bundle = Bundle().apply {
                                        putString("applicationId", doc.id)
                                        putString("ownerName", formData["ownerName"] as? String)
                                        putString("establishmentName", formData["establishmentName"] as? String)
                                        putString("pcoName", formData["pcoName"] as? String)
                                        putString("pcoAccreditationNumber", formData["pcoAccreditation"] as? String)
                                        putString("paymentInfo", "₱1,500 - Pending")
                                    }
                                    findNavController().navigate(R.id.action_form_to_ptoPayment, bundle)
                                }
                                .setNegativeButton("No", null)
                                .show()
                        }

                        // Pending & paid & NOT submitted → continue to review
                        status.equals("Pending", true) && paymentStatus.equals("Paid", true) && submittedTimestamp == null -> {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Paid PTO Found")
                                .setMessage("You have already paid for this PTO application. Do you want to continue to review your application?")
                                .setPositiveButton("Yes") { _, _ ->
                                    val bundle = Bundle().apply {
                                        putString("applicationId", doc.id)
                                        putString("ownerName", formData["ownerName"] as? String)
                                        putString("establishmentName", formData["establishmentName"] as? String)
                                        putString("pcoName", formData["pcoName"] as? String)
                                        putString("pcoAccreditationNumber", formData["pcoAccreditation"] as? String)
                                        putString("paymentInfo", "₱1,500 - Paid")
                                    }
                                    findNavController().navigate(R.id.action_form_to_ptoReview, bundle)
                                }
                                .setNegativeButton("No", null)
                                .show()
                        }

                        // Already submitted or other statuses → do nothing
                        else -> { /* No dialog, user waits for EMB review or can submit a new application */ }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to check previous PTO application.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun validateAndSave() {
        val formData = mapOf(
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
            "applicationType" to "Permit to Operate"
        )

        if (formData.values.any { it.isEmpty() }) {
            Toast.makeText(requireContext(), "Please fill out all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (fileUris.isEmpty()) {
            Toast.makeText(requireContext(), "Please upload at least one supporting file", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmitPto.isEnabled = false
        Toast.makeText(requireContext(), "Uploading files, please wait...", Toast.LENGTH_SHORT).show()

        uploadFilesAndSave(formData)
    }

    private fun uploadFilesAndSave(formData: Map<String, String>) {
        val userUid = auth.currentUser?.uid ?: return
        val uploadTasks = fileUris.map { uri ->
            val ref = storage.reference.child("opms_pto_applications/$userUid/${System.currentTimeMillis()}_${uri.lastPathSegment}")
            ref.putFile(uri).continueWithTask { ref.downloadUrl }
        }

        Tasks.whenAllSuccess<Uri>(uploadTasks)
            .addOnSuccessListener { urls ->
                val fileLinks = urls.map { it.toString() }
                saveFormToFirestore(formData, fileLinks)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "File upload failed. Please check your connection.", Toast.LENGTH_SHORT).show()
                binding.btnSubmitPto.isEnabled = true
            }
    }

    private fun saveFormToFirestore(formData: Map<String, String>, fileLinks: List<String>) {
        val userUid = auth.currentUser?.uid ?: return
        val docId = "${userUid}_${System.currentTimeMillis()}"

        val dataToSave = mutableMapOf<String, Any>()
        dataToSave.putAll(formData)
        dataToSave["fileLinks"] = fileLinks.joinToString(",")
        dataToSave["status"] = "Pending"
        dataToSave["timestamp"] = FieldValue.serverTimestamp()
        dataToSave["uid"] = userUid

        firestore.collection("opms_pto_applications")
            .document(docId)
            .set(dataToSave)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "PTO submitted successfully!", Toast.LENGTH_LONG).show()

                val bundle = Bundle().apply {
                    putString("applicationId", docId)
                    putString("ownerName", formData["ownerName"])
                    putString("establishmentName", formData["establishmentName"])
                    putString("pcoName", formData["pcoName"])
                    putString("pcoAccreditationNumber", formData["pcoAccreditation"])
                    putString("uploadedFiles", fileLinks.joinToString("\n"))
                    putString("paymentInfo", "₱1,500 - Pending")
                }

                clearForm()
                findNavController().navigate(R.id.action_form_to_ptoPayment, bundle)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error submitting PTO. Try again.", Toast.LENGTH_SHORT).show()
                binding.btnSubmitPto.isEnabled = true
            }
    }

    private fun clearForm() {
        binding.apply {
            inputOwnerName.text?.clear()
            inputEstablishmentName.text?.clear()
            inputMailingAddress.text?.clear()
            inputPlantAddress.text?.clear()
            inputTin.text?.clear()
            inputOwnershipType.text?.clear()
            inputNatureOfBusiness.text?.clear()
            inputPcoName.text?.clear()
            inputPcoAccreditation.text?.clear()
            inputOperatingHours.text?.clear()
            inputTotalEmployees.text?.clear()
            inputLandArea.text?.clear()
            inputEquipmentName.text?.clear()
            inputFuelType.text?.clear()
            inputEmissions.text?.clear()
            txtFileName.text = ""
        }
        fileUris.clear()
        binding.btnSubmitPto.isEnabled = false
    }
}
