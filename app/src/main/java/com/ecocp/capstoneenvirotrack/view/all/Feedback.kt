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

class Feedback : Fragment() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnPublish: Button
    private lateinit var etFeedback: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_feedback, container, false)

        // Initialize views
        btnBack = view.findViewById(R.id.btnBack)
        btnPublish = view.findViewById(R.id.btnPublish)
        etFeedback = view.findViewById(R.id.etFeedback)

        // 🔙 Handle Back Button (navigate up)
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // 📝 Enable Publish button only when text is entered
        etFeedback.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnPublish.isEnabled = !s.isNullOrBlank()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 📤 Handle Publish Button click
        btnPublish.setOnClickListener {
            val feedback = etFeedback.text.toString().trim()
            if (feedback.isNotEmpty()) {
                Toast.makeText(requireContext(), "Feedback submitted. Thank you!", Toast.LENGTH_SHORT).show()
                etFeedback.text.clear()
                btnPublish.isEnabled = false
                findNavController().navigateUp() // Optional: Return to previous screen after submit
            }
        }

        return view
    }
}
