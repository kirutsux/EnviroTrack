package com.ecocp.capstoneenvirotrack

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistrationFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var progressDialog: ProgressDialog
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registration, container, false)

        // Initialize Firebase and progress dialog
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        progressDialog = ProgressDialog(requireContext())

        // Find UI elements
        val etEmail: EditText = view.findViewById(R.id.etEmail)
        val etFirstName: EditText = view.findViewById(R.id.etFirstName)
        val etLastName: EditText = view.findViewById(R.id.etLastName)
        val etPassword: EditText = view.findViewById(R.id.etPassword)
        val etConfirmPassword: EditText = view.findViewById(R.id.etConfirmPassword)
        val spUserType: Spinner = view.findViewById(R.id.spUserType)
        val btnSignUp: Button = view.findViewById(R.id.btnSignUp)
        val showPassword1: ImageView = view.findViewById(R.id.showPassword1)
        val showPassword2: ImageView = view.findViewById(R.id.showPassword2)
        val btnLogin: TextView = view.findViewById(R.id.tvLogin)
        val btnBack: ImageView = view.findViewById(R.id.btnBack)

        // Populate Spinner with "Select" as default
        val userTypes = listOf("Select", "Service Provider", "PCO")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, userTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spUserType.adapter = adapter
        spUserType.setSelection(0)

        // Toggle Password Visibility for Password Field
        showPassword1.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(etPassword, showPassword1, isPasswordVisible)
        }

        // Toggle Password Visibility for Confirm Password Field
        showPassword2.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePasswordVisibility(etConfirmPassword, showPassword2, isConfirmPasswordVisible)
        }

        btnLogin.setOnClickListener {
            requireActivity().findNavController(R.id.nav_host_fragment)
                .navigate(R.id.action_registrationFragment_to_loginFragment)
        }

        btnBack.setOnClickListener {
            requireActivity().findNavController(R.id.nav_host_fragment).popBackStack()
        }

        // Handle Email Sign-Up button click
        btnSignUp.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val userType = spUserType.selectedItem.toString()

            if (email.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "All fields are required!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userType == "Select") {
                Toast.makeText(requireContext(), "Please select a valid user type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressDialog.setMessage("Registering...")
            progressDialog.show()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    progressDialog.dismiss()
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            val userMap = hashMapOf(
                                "firstName" to firstName,
                                "lastName" to lastName,
                                "email" to email,
                                "userType" to userType
                            )
                            firestore.collection("Users").document(userId).set(userMap)
                                .addOnSuccessListener {
                                    showSuccessDialog()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        return view
    }

    // ✅ Toggle Password Visibility Function
    private fun togglePasswordVisibility(editText: EditText, toggleIcon: ImageView, isVisible: Boolean) {
        if (isVisible) {
            editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleIcon.setImageResource(R.drawable.ic_visibility_on) // Change to "eye open" icon
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleIcon.setImageResource(R.drawable.ic_visibility_off) // Change to "eye closed" icon
        }
        editText.setSelection(editText.text.length) // Keep cursor at the end
    }

    // ✅ Show Dialog After Successful Registration
    private fun showSuccessDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Registration Successful!")
            .setMessage("Your account has been created successfully.")
            .setPositiveButton("OK") { _, _ ->
                // Navigate to SelectUserType screen
                val intent = Intent(requireContext(), SelectUserType::class.java)
                startActivity(intent)
                requireActivity().finish() // Close current activity
            }
            .setCancelable(false) // Prevent dialog dismissal by clicking outside
            .show()
    }
}
