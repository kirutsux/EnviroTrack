package com.ecocp.capstoneenvirotrack.view.businesses.crs

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class COMP_CRSApplication : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private var selectedFileUri: Uri? = null
    private var uploadedFileUrl: String? = null

    // Input fields
    private lateinit var etCompanyName: TextInputEditText
    private lateinit var etCompanyType: AutoCompleteTextView
    private lateinit var etNatureOfBusiness: AutoCompleteTextView
    private lateinit var etTIN: TextInputEditText
    private lateinit var etStreet: TextInputEditText
    private lateinit var etTelephone: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etWebsite: TextInputEditText
    private lateinit var etRepName: TextInputEditText
    private lateinit var etRepPosition: TextInputEditText
    private lateinit var etRepEmail: TextInputEditText
    private lateinit var etRepContact: TextInputEditText

    // Buttons
    private lateinit var btnUploadEMBID: MaterialButton
    private lateinit var btnSubmit: MaterialButton
    private lateinit var btnClear: MaterialButton
    private lateinit var btnBack: ImageView

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedFileUri = uri
                    val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Selected File"
                    btnUploadEMBID.text = "File Selected ✅ ($fileName)"
                    Toast.makeText(requireContext(), "File selected successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_comp_crs_application, container, false)

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(requireContext()).apply {
            setTitle("Uploading")
            setMessage("Please wait...")
            setCancelable(false)
        }

        // Initialize views
        etCompanyName = view.findViewById(R.id.etCompanyName)
        etCompanyType = view.findViewById(R.id.etCompanyType)
        etNatureOfBusiness = view.findViewById(R.id.etNatureOfBusiness)
        etTIN = view.findViewById(R.id.etTIN)
        etStreet = view.findViewById(R.id.etStreet)
        etTelephone = view.findViewById(R.id.etTelephone)
        etEmail = view.findViewById(R.id.etEmail)
        etWebsite = view.findViewById(R.id.etWebsite)
        etRepName = view.findViewById(R.id.etRepName)
        etRepPosition = view.findViewById(R.id.etRepPosition)
        etRepEmail = view.findViewById(R.id.etRepEmail)
        etRepContact = view.findViewById(R.id.etRepContact)

        btnUploadEMBID = view.findViewById(R.id.etEMBID)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        btnClear = view.findViewById(R.id.btnClear)
        btnBack = view.findViewById(R.id.btnBack)

        // Dropdown setup
        setupDropdownMenus()

        // Listeners
        btnUploadEMBID.setOnClickListener { openFilePicker() }
        btnSubmit.setOnClickListener { validateAndSubmit() }
        btnClear.setOnClickListener { clearFields() }
        btnBack.setOnClickListener { findNavController().navigateUp() }

        return view
    }

    private fun setupDropdownMenus() {
        val companyTypes = listOf("Single Proprietorship", "Partnership", "Corporation", "Cooperative", "Other")
        val natureOfBusinessList = listOf("Manufacturing", "Service", "Trading", "Construction", "Other")

        etCompanyType.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, companyTypes))
        etNatureOfBusiness.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, natureOfBusinessList))
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/*"))
        }
        filePickerLauncher.launch(intent)
    }

    private fun validateAndSubmit() {
        val companyName = etCompanyName.text.toString().trim()
        val companyType = etCompanyType.text.toString().trim()
        val natureOfBusiness = etNatureOfBusiness.text.toString().trim()
        val tin = etTIN.text.toString().trim()
        val address = etStreet.text.toString().trim()

        if (companyName.isEmpty() || companyType.isEmpty() || natureOfBusiness.isEmpty() ||
            tin.isEmpty() || address.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedFileUri == null) {
            Toast.makeText(requireContext(), "Please upload your EMB ID document.", Toast.LENGTH_SHORT).show()
            return
        }

        uploadFileToFirebase(companyName, companyType, natureOfBusiness, tin, address)
    }

    private fun uploadFileToFirebase(
        companyName: String, companyType: String, natureOfBusiness: String,
        tin: String, address: String
    ) {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.show()

        val fileUri = selectedFileUri ?: return
        val fileName = "EMB_ID_${System.currentTimeMillis()}.pdf"
        val storageRef = storage.reference.child("crs_applications/$uid/$fileName")

        storageRef.putFile(fileUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    uploadedFileUrl = uri.toString()
                    saveToFirestore(companyName, companyType, natureOfBusiness, tin, address)
                }
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Upload failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveToFirestore(
        companyName: String, companyType: String, natureOfBusiness: String,
        tin: String, address: String
    ) {
        val uid = auth.currentUser?.uid ?: return

        val applicationData = hashMapOf(
            "userId" to uid,
            "companyName" to companyName,
            "companyType" to companyType,
            "natureOfBusiness" to natureOfBusiness,
            "tin" to tin,
            "address" to address,
            "contactNumber" to etTelephone.text.toString(),
            "email" to etEmail.text.toString(),
            "website" to etWebsite.text.toString(),
            "representativeName" to etRepName.text.toString(),
            "representativePosition" to etRepPosition.text.toString(),
            "representativeEmail" to etRepEmail.text.toString(),
            "representativeContact" to etRepContact.text.toString(),
            "embIDFileUrl" to uploadedFileUrl,
            "status" to "Pending",
            "dateSubmitted" to Date()
        )

        db.collection("crs_applications")
            .add(applicationData)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Application submitted successfully!", Toast.LENGTH_SHORT).show()
                clearFields()
                findNavController().navigateUp()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Error submitting: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun clearFields() {
        listOf(
            etCompanyName, etCompanyType, etNatureOfBusiness, etTIN,
            etStreet, etTelephone, etEmail, etWebsite,
            etRepName, etRepPosition, etRepEmail, etRepContact
        ).forEach { it.text?.clear() }

        btnUploadEMBID.text = "Upload EMB ID Document"
        selectedFileUri = null
    }
}
