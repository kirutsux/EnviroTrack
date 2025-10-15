package com.ecocp.capstoneenvirotrack.view.businesses

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
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

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val PICK_IMAGE_REQUEST = 1001
    private var imageUri: Uri? = null

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

        // 🔙 Back button setup
        ivBack.setOnClickListener {
            findNavController().navigateUp() // This safely goes back to the previous fragment
        }

        // Load existing user data
        fetchUserDetails()

        // Upload photo button functionality
        btnUploadPhoto.setOnClickListener { openImagePicker() }

        // Save changes placeholder
        btnSaveChanges.setOnClickListener {
            Toast.makeText(requireContext(), "Save feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    // 🔹 Open image picker
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // 🔹 Handle image selection result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            imageUri?.let {
                ivProfilePic.setImageURI(it)
                uploadImageToFirebase(it)
            }
        }
    }

    // 🔹 Upload to Firebase Storage
    private fun uploadImageToFirebase(imageUri: Uri) {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = currentUser.uid
        val storageRef = storage.reference.child("profile_pictures/$uid/profile.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()

                    firestore.collection("users").document(uid)
                        .update("profileImageUrl", downloadUrl)
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

    // 🔹 Fetch user data from Firestore
    private fun fetchUserDetails() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = firestore.collection("users").document(uid)
        userRef.get().addOnSuccessListener { document ->
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

                if (profileImageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.sample_profile)
                        .into(ivProfilePic)
                } else {
                    ivProfilePic.setImageResource(R.drawable.sample_profile)
                }

            } else {
                Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error fetching data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
