package com.ecocp.capstoneenvirotrack.view.businesses.crs

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.TextView
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

    private val selectedFileUris = mutableListOf<Uri>()
    private val uploadedFileUrls = mutableListOf<String>()

    // Input fields
    private lateinit var etCompanyName: TextInputEditText
    private lateinit var etCompanyType: AutoCompleteTextView
    private lateinit var etTinNumber: TextInputEditText
    private lateinit var etCeoName: TextInputEditText
    private lateinit var etCeoContact: TextInputEditText
    private lateinit var etNatureOfBusiness: AutoCompleteTextView
    private lateinit var etPsicNo: TextInputEditText
    private lateinit var etIndustryDescriptor: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etWebsite: TextInputEditText
    private lateinit var etRepName: TextInputEditText
    private lateinit var etRepPosition: TextInputEditText
    private lateinit var etRepContact: TextInputEditText
    private lateinit var etRepEmail: TextInputEditText
    private lateinit var txtFileNames: TextView

    // Buttons
    private lateinit var btnUploadFiles: MaterialButton
    private lateinit var btnSaveChanges: MaterialButton
    private lateinit var btnClear: MaterialButton
    private lateinit var btnBack: ImageButton

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        if (!selectedFileUris.contains(uri)) {
                            selectedFileUris.add(uri)
                        }
                    }
                } ?: result.data?.data?.let { uri ->
                    if (!selectedFileUris.contains(uri)) {
                        selectedFileUris.add(uri)
                    }
                }
                updateFileNamesDisplay()
                Toast.makeText(requireContext(), "File(s) selected successfully!", Toast.LENGTH_SHORT).show()
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
        etTinNumber = view.findViewById(R.id.etTinNumber)
        etCeoName = view.findViewById(R.id.etCeoName)
        etCeoContact = view.findViewById(R.id.etCeoContact)
        etNatureOfBusiness = view.findViewById(R.id.etNatureOfBusiness)
        etPsicNo = view.findViewById(R.id.etPsicNo)
        etIndustryDescriptor = view.findViewById(R.id.etIndustryDescriptor)
        etAddress = view.findViewById(R.id.etAddress)
        etPhone = view.findViewById(R.id.etPhone)
        etEmail = view.findViewById(R.id.etEmail)
        etWebsite = view.findViewById(R.id.etWebsite)
        etRepName = view.findViewById(R.id.etRepName)
        etRepPosition = view.findViewById(R.id.etRepPosition)
        etRepContact = view.findViewById(R.id.etRepContact)
        etRepEmail = view.findViewById(R.id.etRepEmail)
        txtFileNames = view.findViewById(R.id.txtFileNames)

        btnUploadFiles = view.findViewById(R.id.btnUploadFiles)
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges)
        btnClear = view.findViewById(R.id.btnClear)
        btnBack = view.findViewById(R.id.btnBack)

        // Dropdown setup
        setupDropdownMenus()

        // Enable/disable Save button based on form input
        setupFormValidation()

        // Listeners
        btnUploadFiles.setOnClickListener { openFilePicker() }
        btnSaveChanges.setOnClickListener { validateAndSubmit() }
        btnClear.setOnClickListener { clearFields() }
        btnBack.setOnClickListener { findNavController().navigateUp() }

        return view
    }

    private fun setupDropdownMenus() {
        val companyTypes = listOf("Single Proprietorship", "Partnership", "Corporation", "Cooperative", "Other")
        val natureOfBusinessList = listOf("Manufacturing", "Service", "Trading", "Construction", "Other")

        etCompanyType.setAdapter(ArrayAdapter(requireContext(), R.layout.dropdown_item, companyTypes))
        etNatureOfBusiness.setAdapter(ArrayAdapter(requireContext(), R.layout.dropdown_item, natureOfBusinessList))
    }

    private fun setupFormValidation() {
        val requiredFields = listOf(
            etCompanyName, etCompanyType, etTinNumber, etCeoName, etCeoContact,
            etNatureOfBusiness, etPsicNo, etIndustryDescriptor, etAddress
        )

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                btnSaveChanges.isEnabled = requiredFields.all { it.text?.toString()?.trim()?.isNotEmpty() == true }
            }
        }

        requiredFields.forEach { field ->
            field.addTextChangedListener(textWatcher)
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/*"))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        filePickerLauncher.launch(intent)
    }

    private fun updateFileNamesDisplay() {
        if (selectedFileUris.isEmpty()) {
            txtFileNames.text = getString(R.string.no_files_selected)
        } else {
            val fileNames = selectedFileUris.map { it.lastPathSegment?.substringAfterLast("/") ?: "Unknown File" }
            txtFileNames.text = fileNames.joinToString("\n")
        }
    }

    private fun validateAndSubmit() {
        val companyName = etCompanyName.text.toString().trim()
        val companyType = etCompanyType.text.toString().trim()
        val tinNumber = etTinNumber.text.toString().trim()
        val ceoName = etCeoName.text.toString().trim()
        val ceoContact = etCeoContact.text.toString().trim()
        val natureOfBusiness = etNatureOfBusiness.text.toString().trim()
        val psicNo = etPsicNo.text.toString().trim()
        val industryDescriptor = etIndustryDescriptor.text.toString().trim()
        val address = etAddress.text.toString().trim()

        if (listOf(companyName, companyType, tinNumber, ceoName, ceoContact, natureOfBusiness, psicNo, industryDescriptor, address).any { it.isEmpty() }) {
            Toast.makeText(requireContext(), "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedFileUris.isEmpty()) {
            Toast.makeText(requireContext(), "Please upload at least one document.", Toast.LENGTH_SHORT).show()
            return
        }

        uploadFilesToFirebase(companyName, companyType, tinNumber, ceoName, ceoContact, natureOfBusiness, psicNo, industryDescriptor, address)
    }

    private fun uploadFilesToFirebase(
        companyName: String, companyType: String, tinNumber: String, ceoName: String, ceoContact: String,
        natureOfBusiness: String, psicNo: String, industryDescriptor: String, address: String
    ) {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.show()
        uploadedFileUrls.clear()

        selectedFileUris.forEachIndexed { index, uri ->
            val fileName = "Document_${System.currentTimeMillis()}_$index.${uri.lastPathSegment?.substringAfterLast(".") ?: "file"}"
            val storageRef = storage.reference.child("crs_applications/$uid/$fileName")

            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        uploadedFileUrls.add(downloadUri.toString())
                        if (uploadedFileUrls.size == selectedFileUris.size) {
                            saveToFirestore(
                                companyName, companyType, tinNumber, ceoName, ceoContact,
                                natureOfBusiness, psicNo, industryDescriptor, address
                            )
                        }
                    }
                }
                .addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Upload failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun saveToFirestore(
        companyName: String, companyType: String, tinNumber: String, ceoName: String, ceoContact: String,
        natureOfBusiness: String, psicNo: String, industryDescriptor: String, address: String
    ) {
        val uid = auth.currentUser?.uid ?: return

        val applicationData = hashMapOf(
            "userId" to uid,
            "companyName" to companyName,
            "companyType" to companyType,
            "tinNumber" to tinNumber,
            "ceoName" to ceoName,
            "ceoContact" to ceoContact,
            "natureOfBusiness" to natureOfBusiness,
            "psicNo" to psicNo,
            "industryDescriptor" to industryDescriptor,
            "address" to address,
            "contactDetails" to mapOf(
                "phone" to etPhone.text.toString().trim(),
                "email" to etEmail.text.toString().trim(),
                "website" to etWebsite.text.toString().trim()
            ),
            "representative" to mapOf(
                "name" to etRepName.text.toString().trim(),
                "position" to etRepPosition.text.toString().trim(),
                "contact" to etRepContact.text.toString().trim(),
                "email" to etRepEmail.text.toString().trim()
            ),
            "fileUrls" to uploadedFileUrls,
            "status" to "Pending",
            "dateSubmitted" to Date()
        )

        db.collection("crs_applications")
            .document(uid)
            .set(applicationData)
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
            etCompanyName, etCompanyType, etTinNumber, etCeoName, etCeoContact,
            etNatureOfBusiness, etPsicNo, etIndustryDescriptor, etAddress,
            etPhone, etEmail, etWebsite, etRepName, etRepPosition, etRepContact, etRepEmail
        ).forEach { it.text?.clear() }

        selectedFileUris.clear()
        uploadedFileUrls.clear()
        txtFileNames.text = getString(R.string.no_files_selected)
        btnSaveChanges.isEnabled = false
    }
}