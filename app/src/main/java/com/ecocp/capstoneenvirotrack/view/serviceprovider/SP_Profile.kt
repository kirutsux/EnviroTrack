package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.view.all.MainActivity
import com.ecocp.capstoneenvirotrack.view.all.Files   // <-- use your existing Files fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SP_Profile : Fragment() {

    private lateinit var ivProfilePic: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_sp_profile, container, false)

        ivProfilePic = view.findViewById(R.id.ivProfilePic)
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)

        // ✅ Go to account fragment
        view.findViewById<View>(R.id.btnAccount).setOnClickListener {
            findNavController().navigate(R.id.SP_Account)
        }

        // ✅ Feedback
        view.findViewById<View>(R.id.btnFeedback).setOnClickListener {
            try {
                val bundle = Bundle().apply { putString("userType", "ServiceProvider") }
                findNavController().navigate(R.id.SP_Feedback, bundle)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Unable to open Feedback. Check nav_graph for SP_Feedback.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // ✅ Files (robust: nav graph preferred, fallback to existing Files fragment)
        view.findViewById<View>(R.id.btnFiles).setOnClickListener {
            // Optional: pass args if you want
            val bundle = Bundle().apply { putString("from", "SP_Profile") }

            try {
                // Preferred: navigate via nav_graph (requires SP_Files destination in nav_graph_sp.xml)
                findNavController().navigate(R.id.SP_Files, bundle)
            } catch (e: Exception) {
                // Fallback: use the existing Files fragment (com.ecocp.capstoneenvirotrack.view.all.Files)
                val filesFragment = Files()
                filesFragment.arguments = bundle

                // try to find a container id (primary navigation fragment's id) to replace
                val containerId = requireActivity().supportFragmentManager.primaryNavigationFragment?.id

                if (containerId != null && containerId != View.NO_ID) {
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(containerId, filesFragment)
                        .addToBackStack(null)
                        .commit()
                } else {
                    // Last fallback: helpful toast so you can add the nav_graph entry
                    Toast.makeText(
                        requireContext(),
                        "Unable to open Files. Add SP_Files to nav_graph or provide a fragment container id.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        // ✅ About Us
        view.findViewById<View>(R.id.btnAboutUs).setOnClickListener {
            try {
                findNavController().navigate(R.id.SP_AboutUs)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Unable to open About Us. Check nav_graph for SP_AboutUs.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // ✅ Logout button
        view.findViewById<View>(R.id.btnLogout).setOnClickListener {
            logoutUser()
        }

        fetchUserProfile()
        return view
    }

    private fun fetchUserProfile() {
        val user = auth.currentUser ?: return
        tvEmail.text = user.email

        db.collection("service_providers").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name")
                    val imageUrl = document.getString("profileImageUrl")

                    tvName.text = name ?: "Service Provider"
                    Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.sample_profile)
                        .error(R.drawable.sample_profile)
                        .into(ivProfilePic)
                }
            }
    }

    private fun logoutUser() {
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        auth.signOut()

        // ✅ Start MainActivity (login host)
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }
}
