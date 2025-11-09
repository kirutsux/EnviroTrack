package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.view.all.MainActivity
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

        // ✅ Start MainActivity, which hosts the LoginFragment
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }



}
