package com.ecocp.capstoneenvirotrack.view.emb.all

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

class EMB_Profile : Fragment() {

    // Views from your exact XML
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var ivProfilePic: CircleImageView

    private lateinit var btnAccount: LinearLayout
    private lateinit var btnFeedback: LinearLayout
    private lateinit var btnAboutUs: LinearLayout
    private lateinit var btnLogout: LinearLayout

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_emb_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // EXACT IDs from your XML — now perfectly matched
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        ivProfilePic = view.findViewById(R.id.ivProfilePic)

        btnAccount = view.findViewById(R.id.btnAccount)
        btnFeedback = view.findViewById(R.id.btnFeedback)
        btnAboutUs = view.findViewById(R.id.btnAboutUs)
        btnLogout = view.findViewById(R.id.btnLogout)

        loadEMBOfficerData()

        // Navigation
        btnAccount.setOnClickListener {
            // findNavController().navigate(R.id.action_embProfile_to_embAccountFragment)
        }

        btnFeedback.setOnClickListener {
            findNavController().navigate(R.id.action_embProfile_to_feedbackFragment)
        }

        btnAboutUs.setOnClickListener {
            findNavController().navigate(R.id.action_embProfile_to_aboutUsFragment)
        }

        btnLogout.setOnClickListener {
            logoutEMBOfficer()
        }
    }

    private fun loadEMBOfficerData() {
        val user = auth.currentUser ?: run {
            tvName.text = "Guest Officer"
            tvEmail.text = "Not signed in"
            return
        }

        tvEmail.text = user.email ?: "officer@emb.gov.ph"

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val firstName = doc.getString("firstName") ?: ""
                    val lastName = doc.getString("lastName") ?: ""
                    val profileUrl = doc.getString("profileImageUrl")

                    val fullName = "$firstName $lastName".trim()
                    tvName.text = if (fullName.isNotBlank()) fullName else "EMB Officer"

                    // Using your real placeholder: sample_profile
                    Glide.with(this)
                        .load(profileUrl ?: R.drawable.sample_profile)
                        .placeholder(R.drawable.sample_profile)
                        .error(R.drawable.sample_profile)
                        .into(ivProfilePic)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logoutEMBOfficer() {
        auth.signOut()
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        findNavController().apply {
            popBackStack(R.id.embDashboard, true)
            navigate(R.id.loginFragment)
        }
    }
}