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


    private lateinit var btnFiles: LinearLayout
    private lateinit var btnAccount: LinearLayout
    private lateinit var btnLogout: LinearLayout
    private lateinit var btnFeedback: LinearLayout
    private lateinit var btnAboutUs: LinearLayout
    private lateinit var btnFaqBot: LinearLayout

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_comp__profile, container, false)

        // Initialize UI
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        ivProfilePic = view.findViewById(R.id.ivProfilePic)
        btnFeedback = view.findViewById(R.id.btnFeedback)
        btnAccount = view.findViewById(R.id.btnAccount)
        btnFiles = view.findViewById(R.id.btnFiles)
        btnAboutUs = view.findViewById(R.id.btnAboutUs)
        btnLogout = view.findViewById(R.id.btnLogout)  // add ID in XML

        loadUserData()

        // 👤 Account
        btnAccount.setOnClickListener {
            findNavController().navigate(R.id.action_COMP_Profile_to_COMP_Account)
        }
        btnFeedback.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_COMP_Profile_to_feedbackFragment)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        btnAboutUs.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_COMP_Profile_to_aboutUsFragment)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        btnFiles.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_COMP_Profile_to_filesFragment)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }


        // 🚪 Logout
        btnLogout.setOnClickListener { logoutUser() }

        return view
    }

    // 👤 Load user data
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

    private fun logoutUser() {
        try {
            auth.signOut()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_COMP_Profile_to_loginFragment)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error logging out: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}