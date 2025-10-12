package com.ecocp.capstoneenvirotrack.view.businesses.hwms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.databinding.FragmentHwmsStep1Binding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class HwmsStep1Fragment : Fragment() {

    private lateinit var binding: FragmentHwmsStep1Binding
    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHwmsStep1Binding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Add first waste row automatically
        addWasteRow(null)

        // Add more waste rows
        binding.btnAddWaste.setOnClickListener {
            addWasteRow(null)
        }

        // Submit button
        binding.btnSubmitStep1.setOnClickListener {
            saveWasteDetails()
        }

        return binding.root
    }

    /**
     * Dynamically adds a new waste entry layout
     */
    private fun addWasteRow(existing: Map<String, String>?) {
        val row = layoutInflater.inflate(R.layout.item_waste_profile, binding.llWastesContainer, false)

        val etName = row.findViewById<EditText>(R.id.etWasteName)
        val etNature = row.findViewById<EditText>(R.id.etNature)
        val etCatalogue = row.findViewById<EditText>(R.id.etCatalogue)
        val etDetails = row.findViewById<EditText>(R.id.etWasteDetails)
        val etPractice = row.findViewById<EditText>(R.id.etCurrentPractice)
        val btnRemove = row.findViewById<ImageButton>(R.id.btnRemoveWaste)

        // Pre-fill existing data if any
        existing?.let {
            etName.setText(it["wasteName"] ?: "")
            etNature.setText(it["nature"] ?: "")
            etCatalogue.setText(it["catalogue"] ?: "")
            etDetails.setText(it["details"] ?: "")
            etPractice.setText(it["currentPractice"] ?: "")
        }

        // Remove waste row
        btnRemove.setOnClickListener {
            binding.llWastesContainer.removeView(row)
        }

        // Add to main container
        binding.llWastesContainer.addView(row)
    }

    /**
     * Gathers all waste data and saves to Firestore
     */
    private fun saveWasteDetails() {
        val container = binding.llWastesContainer
        val wasteList = mutableListOf<Map<String, Any>>()

        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i)

            val wasteName = view.findViewById<EditText>(R.id.etWasteName).text.toString().trim()
            val nature = view.findViewById<EditText>(R.id.etNature).text.toString().trim()
            val catalogue = view.findViewById<EditText>(R.id.etCatalogue).text.toString().trim()
            val details = view.findViewById<EditText>(R.id.etWasteDetails).text.toString().trim()
            val practice = view.findViewById<EditText>(R.id.etCurrentPractice).text.toString().trim()

            if (wasteName.isEmpty() || nature.isEmpty() || catalogue.isEmpty() || details.isEmpty() || practice.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields for each waste entry.", Toast.LENGTH_SHORT).show()
                return
            }

            wasteList.add(
                mapOf(
                    "wasteName" to wasteName,
                    "nature" to nature,
                    "catalogue" to catalogue,
                    "details" to details,
                    "currentPractice" to practice
                )
            )
        }

        if (userId.isEmpty()) {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        // Data to save
        val data = hashMapOf(
            "userId" to userId,
            "wasteDetails" to wasteList,
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to "Draft"
        )

        db.collection("HazardousWasteGenerator")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Waste details saved successfully.", Toast.LENGTH_SHORT).show()
                // TODO: navigate to Step 2 fragment
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save waste details.", Toast.LENGTH_SHORT).show()
            }
    }
}
