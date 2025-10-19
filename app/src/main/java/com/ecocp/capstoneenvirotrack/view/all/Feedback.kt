package com.ecocp.capstoneenvirotrack.view.all

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class Feedback : Fragment() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnPublish: Button
    private lateinit var etFeedback: EditText
    private lateinit var tvCharCount: TextView
    private val maxChars = 500

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_feedback, container, false)

        // Initialize views
        btnBack = view.findViewById(R.id.btnBack)
        btnPublish = view.findViewById(R.id.btnPublish)
        etFeedback = view.findViewById(R.id.etFeedback)
        tvCharCount = view.findViewById(R.id.tvCharCount)

        // 🔙 Back button navigation
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // 📝 Character count + enable publish logic
        etFeedback.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                tvCharCount.text = "$length/$maxChars"
                btnPublish.isEnabled = length in 1..maxChars
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // ✅ Handle Publish Button
        btnPublish.setOnClickListener {
            val feedbackText = etFeedback.text.toString().trim()
            if (feedbackText.isNotEmpty()) {
                saveFeedbackToFirestore(feedbackText)
            }
        }

        return view
    }

    // 🔥 Save feedback → feedback/{uid}/entries/{autoId}
    private fun saveFeedbackToFirestore(feedback: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = user.uid
        val firestore = FirebaseFirestore.getInstance()

        // Retrieve userType from users/{uid}
        val userRef = firestore.collection("users").document(uid)
        userRef.get().addOnSuccessListener { document ->
            val userType = document.getString("userType") ?: "Unknown"

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            val feedbackData = mapOf(
                "message" to feedback,
                "timestamp" to timestamp,
                "userType" to userType,
                "uid" to uid
            )

            // Save under feedback/{uid}/entries/{autoId}
            firestore.collection("feedback")
                .document(uid)
                .collection("entries")
                .add(feedbackData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Feedback submitted successfully!", Toast.LENGTH_SHORT).show()
                    etFeedback.text.clear()
                    btnPublish.isEnabled = false
                    findNavController().navigateUp()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }

        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to retrieve user type", Toast.LENGTH_SHORT).show()
        }
    }
}
