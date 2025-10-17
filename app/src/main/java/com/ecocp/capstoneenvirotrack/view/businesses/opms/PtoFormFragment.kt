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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
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
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("opms_pto_applications")
            .whereEqualTo("uid", uid)
            .whereEqualTo("status", "Pending")
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Pending PTO Found")
                        .setMessage("You already have a pending PTO application. Resume to review/payment?")
                        .setPositiveButton("Yes") { _, _ ->
                            val doc = snap.documents.first()
                            val bundle = Bundle().apply { putString("applicationId", doc.id) }
                            findNavController().navigate(R.id.action_form_to_ptoPayment, bundle)
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            }
    }

    private fun validateAndSave() {
        val ownerName = binding.inputOwnerName.text.toString().trim()
        val establishmentName = binding.inputEstablishmentName.text.toString().trim()
        val mailingAddress = binding.inputMailingAddress.text.toString().trim()
        val plantAddress = binding.inputPlantAddress.text.toString().trim()
        val tin = binding.inputTin.text.toString().trim()
        val ownershipType = binding.inputOwnershipType.text.toString().trim()
        val natureOfBusiness = binding.inputNatureOfBusiness.text.toString().trim()
        val pcoName = binding.inputPcoName.text.toString().trim()
        val pcoAccreditation = binding.inputPcoAccreditation.text.toString().trim()
        val operatingHours = binding.inputOperatingHours.text.toString().trim()
        val totalEmployees = binding.inputTotalEmployees.text.toString().trim()
        val landArea = binding.inputLandArea.text.toString().trim()
        val equipmentName = binding.inputEquipmentName.text.toString().trim()
        val fuelType = binding.inputFuelType.text.toString().trim()
        val emissions = binding.inputEmissions.text.toString().trim()

        if (ownerName.isEmpty() || establishmentName.isEmpty() || mailingAddress.isEmpty() ||
            plantAddress.isEmpty() || tin.isEmpty() || ownershipType.isEmpty() ||
            natureOfBusiness.isEmpty() || pcoName.isEmpty() || pcoAccreditation.isEmpty() ||
            operatingHours.isEmpty() || totalEmployees.isEmpty() || landArea.isEmpty() ||
            equipmentName.isEmpty() || fuelType.isEmpty() || emissions.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Please fill out all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        if (fileUris.isEmpty()) {
            Toast.makeText(requireContext(), "Please upload at least one supporting file", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmitPto.isEnabled = false
        Toast.makeText(requireContext(), "Uploading files, please wait...", Toast.LENGTH_SHORT).show()

        val uploadTasks = fileUris.map { uri ->
            val ref = storage.reference.child("opms_pto_applications/$uid/${System.currentTimeMillis()}_${uri.lastPathSegment}")
            ref.putFile(uri).continueWithTask { ref.downloadUrl }
        }

        Tasks.whenAllSuccess<Uri>(uploadTasks)
            .addOnSuccessListener { urls ->
                val fileLinks = urls.map { it.toString() }
                val data = mutableMapOf<String, Any>(
                    "uid" to uid,
                    "ownerName" to ownerName,
                    "establishmentName" to establishmentName,
                    "mailingAddress" to mailingAddress,
                    "plantAddress" to plantAddress,
                    "tin" to tin,
                    "ownershipType" to ownershipType,
                    "natureOfBusiness" to natureOfBusiness,
                    "pcoName" to pcoName,
                    "pcoAccreditation" to pcoAccreditation,
                    "operatingHours" to operatingHours,
                    "totalEmployees" to totalEmployees,
                    "landArea" to landArea,
                    "equipmentName" to equipmentName,
                    "fuelType" to fuelType,
                    "emissionsSummary" to emissions,
                    "fileLinks" to fileLinks.joinToString(","),
                    "status" to "Pending",
                    "timestamp" to Timestamp.now(),
                    "applicationType" to "Permit to Operate"
                )

                firestore.collection("opms_pto_applications")
                    .add(data)
                    .addOnSuccessListener { docRef ->
                        Toast.makeText(requireContext(), "Application saved successfully. Proceeding to Payment...", Toast.LENGTH_SHORT).show()
                        val bundle = Bundle().apply { putString("applicationId", docRef.id) }
                        findNavController().navigate(R.id.action_form_to_ptoPayment, bundle)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                        binding.btnSubmitPto.isEnabled = true
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "File upload failed. Please check your network.", Toast.LENGTH_SHORT).show()
                binding.btnSubmitPto.isEnabled = true
            }
    }
}
