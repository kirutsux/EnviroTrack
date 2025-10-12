package com.ecocp.capstoneenvirotrack.view.businesses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class COMP_Profile : Fragment() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var ivProfilePic: CircleImageView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_comp__profile, container, false)

        // Initialize UI elements
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        ivProfilePic = view.findViewById(R.id.ivProfilePic)

        // Load user data from Firebase
        loadUserData()

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
