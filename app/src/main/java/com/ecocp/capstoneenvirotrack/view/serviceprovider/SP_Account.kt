package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

class SP_Account : Fragment() {

    private lateinit var etName: EditText
    private lateinit var etCompanyName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etContact: EditText
    private lateinit var etLocation: EditText
    private lateinit var etRole: EditText
    private lateinit var ivProfilePic: CircleImageView
    private lateinit var btnUploadPhoto: Button
    private lateinit var btnSaveChanges: Button
    private lateinit var ivBack: ImageView
    private lateinit var showPassword: ImageView
    private lateinit var progressBar: ProgressBar

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val PICK_IMAGE_REQUEST = 1001
    private var imageUri: Uri? = null
    private var originalData: Map<String, String> = emptyMap()
    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sp_account, container, false)

        // Initialize views
        etName = view.findViewById(R.id.etName)
        etCompanyName = view.findViewById(R.id.etCompanyName)
        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        etContact = view.findViewById(R.id.etContact)
        etLocation = view.findViewById(R.id.etLocation)
        etRole = view.findViewById(R.id.etRole)
        ivProfilePic = view.findViewById(R.id.ivProfilePic)
        btnUploadPhoto = view.findViewById(R.id.btnUploadPhoto)
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges)
        ivBack = view.findViewById(R.id.ivBack)
        showPassword = view.findViewById(R.id.showPassword)
        progressBar = view.findViewById(R.id.progressBar)

        // Hide bottom navigation safely
        activity?.findViewById<BottomNavigationView>(R.id.spBottomNavigation)?.visibility = View.GONE

        // Disable uneditable fields
        etEmail.isEnabled = false
        etRole.isEnabled = false

        // Disable save button initially
        btnSaveChanges.isEnabled = false
        btnSaveChanges.alpha = 0.5f

        // 🔙 Safe back navigation
        ivBack.setOnClickListener {
            activity?.let { act ->
                act.onBackPressedDispatcher.onBackPressed()
                act.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }

        btnUploadPhoto.setOnClickListener { openImagePicker() }
        btnSaveChanges.setOnClickListener { saveUserChanges() }

        // 👁️ Password visibility toggle
        showPassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(etPassword, showPassword, isPasswordVisible)
        }

        fetchSPDetails()
        setupTextWatchers()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Safely show bottom navigation again when leaving
        activity?.let { act ->
            act.findViewById<BottomNavigationView>(R.id.spBottomNavigation)?.visibility = View.VISIBLE
            act.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun togglePasswordVisibility(editText: EditText, toggleIcon: ImageView, isVisible: Boolean) {
        if (isVisible) {
            editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleIcon.setImageResource(R.drawable.ic_visibility_on)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleIcon.setImageResource(R.drawable.ic_visibility_off)
        }
        editText.setSelection(editText.text.length)
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = checkForChanges()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        etName.addTextChangedListener(watcher)
        etCompanyName.addTextChangedListener(watcher)
        etPassword.addTextChangedListener(watcher)
        etContact.addTextChangedListener(watcher)
        etLocation.addTextChangedListener(watcher)
    }

    private fun checkForChanges() {
        val currentData = mapOf(
            "name" to etName.text.toString().trim(),
            "companyName" to etCompanyName.text.toString().trim(),
            "password" to etPassword.text.toString().trim(),
            "contactNumber" to etContact.text.toString().trim(),
            "location" to etLocation.text.toString().trim()
        )
        val hasChanges = currentData != originalData
        btnSaveChanges.isEnabled = hasChanges
        btnSaveChanges.alpha = if (hasChanges) 1f else 0.5f
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
        activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            imageUri?.let {
                ivProfilePic.setImageURI(it)
                uploadImageToFirebase(it)
                enableSaveButtonTemporarily()
            }
        }
    }

    private fun enableSaveButtonTemporarily() {
        btnSaveChanges.isEnabled = true
        btnSaveChanges.alpha = 1f
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        val storageRef = storage.reference.child("sp_profile_pictures/$uid/profile.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    firestore.collection("service_providers").document(uid)
                        .update("profileImageUrl", uri.toString())
                        .addOnSuccessListener {
                            if (isAdded) {
                                Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show()
                                fetchSPDetails()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun fetchSPDetails() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("service_providers").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("name") ?: ""
                    val companyName = doc.getString("companyName") ?: ""
                    val email = doc.getString("email") ?: ""
                    val password = doc.getString("password") ?: ""
                    val contactNumber = doc.getString("contactNumber") ?: ""
                    val location = doc.getString("location") ?: ""
                    val role = doc.getString("role") ?: ""
                    val profileImageUrl = doc.getString("profileImageUrl") ?: ""

                    etName.setText(name)
                    etCompanyName.setText(companyName)
                    etEmail.setText(email)
                    etPassword.setText(password)
                    etContact.setText(contactNumber)
                    etLocation.setText(location)
                    etRole.setText(role)

                    originalData = mapOf(
                        "name" to name,
                        "companyName" to companyName,
                        "password" to password,
                        "contactNumber" to contactNumber,
                        "location" to location
                    )

                    if (isAdded) {
                        if (profileImageUrl.isNotEmpty()) {
                            Glide.with(this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.sample_profile)
                                .into(ivProfilePic)
                        } else {
                            ivProfilePic.setImageResource(R.drawable.sample_profile)
                        }
                    }

                    checkForChanges()
                } else if (isAdded) {
                    Toast.makeText(requireContext(), "Service Provider data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error fetching data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserChanges() {
        val user = auth.currentUser ?: return
        val name = etName.text.toString().trim()
        val companyName = etCompanyName.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val contact = etContact.text.toString().trim()
        val location = etLocation.text.toString().trim()

        if (name.isEmpty() || companyName.isEmpty() || password.isEmpty()) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Name, company name, and password are required", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (companyName.isEmpty() || password.isEmpty()) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Company name and password are required", Toast.LENGTH_SHORT).show()
            }
            return
        }

        progressBar.visibility = View.VISIBLE
        btnSaveChanges.isEnabled = false
        btnSaveChanges.alpha = 0.5f

        val userRef = firestore.collection("service_providers").document(user.uid)
        val updates = mapOf(
            "name" to name,
            "companyName" to companyName,
            "password" to password,
            "contactNumber" to contact,
            "location" to location
        )
        userRef.update(updates)
            .addOnSuccessListener {
                user.updatePassword(password)
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Account updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                        originalData = updates
                        checkForChanges()
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility = View.GONE
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Failed to update Auth password: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to update account: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                checkForChanges()
            }
    }
}
