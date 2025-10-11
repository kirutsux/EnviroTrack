package com.ecocp.capstoneenvirotrack.view.businesses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class COMP_Account : Fragment() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etContact: EditText
    private lateinit var ivProfilePic: CircleImageView
    private lateinit var btnSaveChanges: Button

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_comp__account, container, false)

        // Initialize views
        etName = view.findViewById(R.id.etName)
        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        etContact = view.findViewById(R.id.etContact)
        ivProfilePic = view.findViewById(R.id.ivProfilePic)
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges)

        fetchUserDetails()

        btnSaveChanges.setOnClickListener {
            Toast.makeText(requireContext(), "Save feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        return view
    }

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
                etContact.setText(phoneNumber) // using phone number as placeholder for address

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
