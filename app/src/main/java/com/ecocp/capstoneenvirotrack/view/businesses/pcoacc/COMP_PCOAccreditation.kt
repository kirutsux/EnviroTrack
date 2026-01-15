package com.ecocp.capstoneenvirotrack.view.businesses.pcoacc

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.api.PcoSendNotificationRequest
import com.ecocp.capstoneenvirotrack.api.RetrofitClient
import com.ecocp.capstoneenvirotrack.utils.NotificationManager
import com.ecocp.capstoneenvirotrack.view.all.COMP_PCO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject
import retrofit2.Call
import java.text.SimpleDateFormat
import java.util.*

class COMP_PCOAccreditation : Fragment() {

    private lateinit var fullName: EditText
    private lateinit var positionDesignation: AutoCompleteTextView
    private lateinit var accreditationNumber: EditText
    private lateinit var companyAffiliation: EditText
    private lateinit var educationalBackground: AutoCompleteTextView
    private lateinit var experienceInEnvManagement: AutoCompleteTextView
    private lateinit var uploadCertificateButton: Button
    private lateinit var uploadGovernmentIDButton: Button
    private lateinit var uploadTrainingCertificateButton: Button
    private lateinit var submitApplicationButton: Button
    private lateinit var progressDialog: ProgressDialog
    private lateinit var backButton: ImageView

    private var certificateUri: Uri? = null
    private var governmentIdUri: Uri? = null
    private var trainingCertUri: Uri? = null

    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val PICK_CERTIFICATE_REQUEST = 1
        private const val PICK_GOV_ID_REQUEST = 2
        private const val PICK_TRAINING_CERT_REQUEST = 3
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.pco_accreditation, container, false)

        fullName = view.findViewById(R.id.fullName)
        positionDesignation = view.findViewById(R.id.positionDesignation)
        accreditationNumber = view.findViewById(R.id.accreditationNumber)
        companyAffiliation = view.findViewById(R.id.companyAffiliation)
        educationalBackground = view.findViewById(R.id.educationalBackground)
        experienceInEnvManagement = view.findViewById(R.id.experienceInEnvManagement)
        uploadCertificateButton = view.findViewById(R.id.uploadCertificateButton)
        uploadGovernmentIDButton = view.findViewById(R.id.uploadGovernmentIDButton)
        uploadTrainingCertificateButton = view.findViewById(R.id.uploadTrainingCertificateButton)
        submitApplicationButton = view.findViewById(R.id.submitApplicationButton)
        backButton = view.findViewById(R.id.backButton)

        backButton.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }

        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Submitting Application...")
            setCancelable(false)
        }

        setupDropdowns()
        setupButtonListeners()
        return view
    }

    private fun setupDropdowns() {
        // Position / Designation Dropdown
        val positionArray = resources.getStringArray(R.array.pco_position_designation)
        val positionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            positionArray
        )
        positionDesignation.setAdapter(positionAdapter)

        // Educational Background Dropdown
        val educationArray = resources.getStringArray(R.array.pco_educational_background)
        val educationAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            educationArray
        )
        educationalBackground.setAdapter(educationAdapter)

        // Experience in Environmental Management Dropdown
        val experienceArray = resources.getStringArray(R.array.pco_experience_env_management)
        val experienceAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            experienceArray
        )
        experienceInEnvManagement.setAdapter(experienceAdapter)
    }

    private fun setupButtonListeners() {
        uploadCertificateButton.setOnClickListener { openFileChooser(PICK_CERTIFICATE_REQUEST) }
        uploadGovernmentIDButton.setOnClickListener { openFileChooser(PICK_GOV_ID_REQUEST) }
        uploadTrainingCertificateButton.setOnClickListener { openFileChooser(PICK_TRAINING_CERT_REQUEST) }

        submitApplicationButton.setOnClickListener {
            if (validateInputs()) uploadFilesAndSaveData()
        }
    }

    private fun openFileChooser(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "application/pdf" }
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data?.data != null) {
            val uri = data.data
            val fileName = getFileNameFromUri(uri)
            when (requestCode) {
                PICK_CERTIFICATE_REQUEST -> {
                    certificateUri = uri
                    uploadCertificateButton.text = fileName ?: "Certificate Selected"
                }
                PICK_GOV_ID_REQUEST -> {
                    governmentIdUri = uri
                    uploadGovernmentIDButton.text = fileName ?: "Government ID Selected"
                }
                PICK_TRAINING_CERT_REQUEST -> {
                    trainingCertUri = uri
                    uploadTrainingCertificateButton.text = fileName ?: "Training Certificate Selected"
                }
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri?): String? {
        uri ?: return null
        var name: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) name = it.getString(nameIndex)
            }
        }
        return name
    }

    private fun validateInputs(): Boolean {
        if (fullName.text.isEmpty() ||
            positionDesignation.text.isEmpty() ||
            companyAffiliation.text.isEmpty() ||
            educationalBackground.text.isEmpty() ||
            experienceInEnvManagement.text.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (certificateUri == null || governmentIdUri == null || trainingCertUri == null) {
            Toast.makeText(requireContext(), "Please upload all required documents.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun uploadFilesAndSaveData() {
        val uid = auth.currentUser?.uid ?: return
        progressDialog.show()

        val accreditationId = UUID.randomUUID().toString()
        val userFolderRef = storage.reference.child("accreditations/$uid/$accreditationId")

        val certificateRef = userFolderRef.child("certificate.pdf")
        val govIdRef = userFolderRef.child("government_id.pdf")
        val trainingCertRef = userFolderRef.child("training_certificate.pdf")

        certificateRef.putFile(certificateUri!!)
            .continueWithTask { certificateRef.downloadUrl }
            .addOnSuccessListener { certificateUrl ->
                govIdRef.putFile(governmentIdUri!!)
                    .continueWithTask { govIdRef.downloadUrl }
                    .addOnSuccessListener { govIdUrl ->
                        trainingCertRef.putFile(trainingCertUri!!)
                            .continueWithTask { trainingCertRef.downloadUrl }
                            .addOnSuccessListener { trainingCertUrl ->
                                saveAccreditationData(
                                    uid,
                                    accreditationId,
                                    certificateUrl.toString(),
                                    govIdUrl.toString(),
                                    trainingCertUrl.toString()
                                )
                            }
                    }
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveAccreditationData(
        uid: String,
        accreditationId: String,
        certUrl: String,
        govIdUrl: String,
        trainingUrl: String
    ) {
        // ✅ Format timestamp
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' h:mm:ss a 'UTC+8'", Locale.ENGLISH)
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Manila")
        val formattedTimestamp = dateFormat.format(Date())

        // ✅ Prepare "Pending" application data
        val data = hashMapOf(
            "accreditationId" to accreditationId,
            "fullName" to fullName.text.toString(),
            "positionDesignation" to positionDesignation.text.toString(),
            "accreditationNumber" to accreditationNumber.text.toString(),
            "companyAffiliation" to companyAffiliation.text.toString(),
            "educationalBackground" to educationalBackground.text.toString(),
            "experienceInEnvManagement" to experienceInEnvManagement.text.toString(),
            "certificateUrl" to certUrl,
            "governmentIdUrl" to govIdUrl,
            "trainingCertificateUrl" to trainingUrl,
            "uid" to uid,
            "status" to "Pending",
            "submittedTimestamp" to formattedTimestamp
        )

        firestore.collection("accreditations")
            .document(accreditationId)
            .set(data)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Application submitted successfully!", Toast.LENGTH_LONG).show()

                // --------------------------------------------------------
                // 🔔 CALL BACKEND API — NOTIFY PCO + ALL EMB USERS
                // --------------------------------------------------------
                val request = PcoSendNotificationRequest(
                    receiverId = uid,          // PCO UID
                    module = "PCO", // Module name
                    documentId = accreditationId  // Firestore document ID
                )

                RetrofitClient.instance.sendPcoSubmissionNotification(request)
                    .enqueue(object : retrofit2.Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: retrofit2.Response<Void>) {
                            if (response.isSuccessful) {
                                Log.d("NOTIF", "Accreditation submission notifications sent.")
                            } else {
                                Log.e("NOTIF", "Failed to send notifications: ${response.code()}")
                                Toast.makeText(requireContext(), "Notification error: ${response.code()}", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            Log.e("NOTIF", "Error sending notifications: ${t.message}")
                            Toast.makeText(requireContext(), "Failed to send notifications", Toast.LENGTH_SHORT).show()
                        }
                    })

                // ✅ SAFE to navigate AFTER submission
                findNavController().navigate(
                    R.id.COMP_PCO,   // target fragment
                    null,                        // optional bundle
                    NavOptions.Builder()
                        .setPopUpTo(R.id.COMP_PCO, true)  // clear back stack
                        .build()
                )

            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Failed to save application: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}