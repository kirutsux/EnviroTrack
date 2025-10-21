package com.ecocp.capstoneenvirotrack.view.all

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Feedback : Fragment() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnPublish: Button
    private lateinit var etFeedback: EditText

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_feedback, container, false)

        btnBack = view.findViewById(R.id.btnBack)
        btnPublish = view.findViewById(R.id.btnPublish)
        etFeedback = view.findViewById(R.id.etFeedback)

        btnPublish.isEnabled = false

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        etFeedback.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnPublish.isEnabled = !s.isNullOrBlank()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnPublish.setOnClickListener {
            submitFeedback()
        }

        return view
    }

    private fun submitFeedback() {
        val feedbackText = etFeedback.text.toString().trim()
        val user = auth.currentUser

        if (feedbackText.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter your feedback.", Toast.LENGTH_SHORT).show()
            return
        }

        if (user == null) {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        // Retrieve userType directly from Firestore
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val userType = document.getString("userType") ?: "unknown"

                val feedbackData = hashMapOf(
                    "message" to feedbackText,
                    "timestamp" to Timestamp.now(),
                    "uid" to user.uid,
                    "userType" to userType
                )

                // ✅ matches your rules
                db.collection("feedback")
                    .document(user.uid)
                    .collection("entries")
                    .add(feedbackData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Feedback submitted. Thank you!", Toast.LENGTH_SHORT).show()
                        etFeedback.text.clear()
                        btnPublish.isEnabled = false
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error submitting feedback: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to retrieve user type.", Toast.LENGTH_SHORT).show()
            }
    }
}
