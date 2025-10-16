package com.ecocp.capstoneenvirotrack.view.businesses

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

class COMP_Account : Fragment() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etContact: EditText
    private lateinit var ivProfilePic: CircleImageView
    private lateinit var btnUploadPhoto: Button
    private lateinit var btnSaveChanges: Button
    private lateinit var ivBack: ImageView
    private lateinit var progressBar: ProgressBar

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val PICK_IMAGE_REQUEST = 1001
    private var imageUri: Uri? = null
    private var originalData: Map<String, String> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.pco_account, container, false)

        // Initialize views
        etName = view.findViewById(R.id.etName)
        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        etContact = view.findViewById(R.id.etContact)
        ivProfilePic = view.findViewById(R.id.ivProfilePic)
        btnUploadPhoto = view.findViewById(R.id.btnUploadPhoto)
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges)
        ivBack = view.findViewById(R.id.ivBack)
        progressBar = view.findViewById(R.id.progressBar)

        // Disable email editing
        etEmail.isEnabled = false
        etEmail.isFocusable = false
        etEmail.isClickable = false

        // Disable save button by default
        btnSaveChanges.isEnabled = false
        btnSaveChanges.alpha = 0.5f

        ivBack.setOnClickListener { findNavController().navigateUp() }
        btnUploadPhoto.setOnClickListener { openImagePicker() }
        btnSaveChanges.setOnClickListener { saveUserChanges() }

        fetchUserDetails()
        setupTextWatchers()

        return view
    }

    // 🔹 Monitor if user edits anything
    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                checkForChanges()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        etName.addTextChangedListener(watcher)
        etPassword.addTextChangedListener(watcher)
        etContact.addTextChangedListener(watcher)
    }

    private fun checkForChanges() {
        val currentData = mapOf(
            "name" to etName.text.toString().trim(),
            "password" to etPassword.text.toString().trim(),
            "contact" to etContact.text.toString().trim()
        )

        val hasChanges = currentData != originalData
        btnSaveChanges.isEnabled = hasChanges
        btnSaveChanges.alpha = if (hasChanges) 1.0f else 0.5f
    }

    // 🔹 Open Image Picker
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // 🔹 Handle Image Selection Result
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
        btnSaveChanges.alpha = 1.0f
    }

    // 🔹 Upload Image to Firebase Storage
    private fun uploadImageToFirebase(imageUri: Uri) {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        val storageRef = storage.reference.child("profile_pictures/$uid/profile.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    firestore.collection("users").document(uid)
                        .update("profileImageUrl", uri.toString())
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show()
                            fetchUserDetails()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to update Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // 🔹 Fetch User Details
    private fun fetchUserDetails() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val email = document.getString("email") ?: ""
                    val password = document.getString("password") ?: ""
                    val phoneNumber = document.getString("phoneNumber") ?: ""
                    val profileImageUrl = document.getString("profileImageUrl") ?: ""

                    etName.setText("$firstName $lastName")
                    etEmail.setText(email)
                    etPassword.setText(password)
                    etContact.setText(phoneNumber)

                    // Save original data for change detection
                    originalData = mapOf(
                        "name" to "$firstName $lastName",
                        "password" to password,
                        "contact" to phoneNumber
                    )

                    if (profileImageUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.sample_profile)
                            .into(ivProfilePic)
                    } else {
                        ivProfilePic.setImageResource(R.drawable.sample_profile)
                    }

                    checkForChanges()
                } else {
                    Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // 🔹 Save Changes (Firestore only)
    private fun saveUserChanges() {
        val user = auth.currentUser ?: return

        val fullName = etName.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val contact = etContact.text.toString().trim()

        if (fullName.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Name and password are required", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnSaveChanges.isEnabled = false
        btnSaveChanges.alpha = 0.5f

        val nameParts = fullName.split(" ", limit = 2)
        val firstName = nameParts.getOrNull(0) ?: ""
        val lastName = nameParts.getOrNull(1) ?: ""

        val userRef = firestore.collection("users").document(user.uid)
        val updates = mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "password" to password,
            "phoneNumber" to contact
        )

        userRef.update(updates)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Account updated successfully!", Toast.LENGTH_SHORT).show()
                originalData = mapOf(
                    "name" to fullName,
                    "password" to password,
                    "contact" to contact
                )
                checkForChanges()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to update account: ${e.message}", Toast.LENGTH_SHORT).show()
                checkForChanges()
            }
    }
}
