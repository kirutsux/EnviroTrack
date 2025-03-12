package com.ecocp.capstoneenvirotrack

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegistrationFragment : Fragment() {
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registration, container, false)

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().reference.child("Users")

        // Find UI elements
        val etEmail: EditText = view.findViewById(R.id.etEmail)
        val etFirstName: EditText = view.findViewById(R.id.etFirstName)
        val etLastName: EditText = view.findViewById(R.id.etLastName)
        val etPassword: EditText = view.findViewById(R.id.etPassword)
        val rbMale: RadioButton = view.findViewById(R.id.rbMale)
        val rbFemale: RadioButton = view.findViewById(R.id.rbFemale)
        val spUserType: Spinner = view.findViewById(R.id.spUserType)
        val btnSignUp: Button = view.findViewById(R.id.btnSignUp)

        // Populate Spinner with "Select" as default
        val userTypes = listOf("Select", "Service Provider", "PCO")
        val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, userTypes) {
            override fun isEnabled(position: Int): Boolean {
                // Disable the "Select" option
                return position != 0
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as TextView).setTextColor(if (position == 0) Color.GRAY else Color.BLACK)
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spUserType.adapter = adapter
        spUserType.setSelection(0) // Default selection to "Select"

        // Handle Sign Up button click
        btnSignUp.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val gender = when {
                rbMale.isChecked -> "Male"
                rbFemale.isChecked -> "Female"
                else -> "Not specified"
            }
            val userType = spUserType.selectedItem.toString()

            if (email.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (userType == "Select") {
                Toast.makeText(requireContext(), "Please select a valid user type", Toast.LENGTH_SHORT).show()
            } else {
                saveUserToFirebase(email, firstName, lastName, password, gender, userType)
            }
        }

        return view
    }

    private fun saveUserToFirebase(email: String, firstName: String, lastName: String, password: String, gender: String, userType: String) {
        val userId = database.push().key ?: return
        val user = User(userId, email, firstName, lastName, password, gender, userType)

        database.child(userId).setValue(user).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "Registration Successful!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to register. Try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
