package com.ecocp.capstoneenvirotrack.view.businesses

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

class COMP_Profile : Fragment() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var ivProfilePic: CircleImageView
    private lateinit var ivEditPic: ImageView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val PICK_IMAGE_REQUEST = 1001
    private var imageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_comp__profile, container, false)

        // Initialize UI elements
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        ivProfilePic = view.findViewById(R.id.ivProfilePic)
        ivEditPic = view.findViewById(R.id.ivEditPic)

        // Load user data from Firebase
        loadUserData()

        // Edit profile picture
        ivEditPic.setOnClickListener { openImagePicker() }

        // 🔹 Navigate to Account Fragment
        val btnAccount = view.findViewById<LinearLayout>(R.id.btnAccount)
        btnAccount.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_COMP_Profile_to_COMP_Account)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    // 🖼️ Open Image Picker
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // 📥 Handle Image Picker Result
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

    // ☁️ Upload Image to Firebase Storage
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

                    db.collection("users").document(uid)
                        .update("profileImageUrl", downloadUrl)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show()
                            loadUserData()
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

    // 👤 Load user info from Firestore
    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            tvEmail.text = currentUser.email ?: "No Email Available"
            val uid = currentUser.uid

            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        val profileImageUrl = document.getString("profileImageUrl")
                        val fullName = "$firstName $lastName".trim()

                        tvName.text = if (fullName.isNotBlank()) fullName else "Unknown User"

                        Glide.with(this)
                            .load(profileImageUrl ?: R.drawable.sample_profile)
                            .placeholder(R.drawable.sample_profile)
                            .into(ivProfilePic)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to load user info: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            tvName.text = "Guest"
            tvEmail.text = "Not signed in"
        }
    }
}
