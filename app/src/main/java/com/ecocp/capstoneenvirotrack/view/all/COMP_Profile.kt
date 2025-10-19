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

    private lateinit var btnAccount: LinearLayout
    private lateinit var btnLogout: LinearLayout
    private lateinit var btnFeedback: LinearLayout
    private lateinit var btnModules: LinearLayout
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
        btnAccount = view.findViewById(R.id.btnAccount)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnFeedback = view.findViewById(R.id.btnFeedback)  // add ID in XML
        btnModules = view.findViewById(R.id.btnModules)    // add ID in XML
        btnAboutUs = view.findViewById(R.id.btnAboutUs)    // add ID in XML
        btnFaqBot = view.findViewById(R.id.btnFaqBot)      // add ID in XML

        loadUserData()

        // 👤 Account
        btnAccount.setOnClickListener {
            findNavController().navigate(R.id.action_COMP_Profile_to_COMP_Account)
        }

        // 💬 Feedback
        btnFeedback.setOnClickListener {
            findNavController().navigate(R.id.feedbackFragment)
        }

        // 📘 Modules
        btnModules.setOnClickListener {
            findNavController().navigate(R.id.modulesFragment)
        }

        // ℹ️ About Us
        btnAboutUs.setOnClickListener {
            findNavController().navigate(R.id.aboutUsFragment)
        }

        // 🤖 AI FAQ Bot
        btnFaqBot.setOnClickListener {
            findNavController().navigate(R.id.aiFaqBotFragment)
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
