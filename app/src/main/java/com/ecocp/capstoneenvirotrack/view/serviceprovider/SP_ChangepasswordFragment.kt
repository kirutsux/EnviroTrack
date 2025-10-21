package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SP_ChangepasswordFragment : Fragment() {

    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnUpdatePassword: Button
    private lateinit var tvLogin: TextView
    private lateinit var spinnerRole: Spinner

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.sp_changepassword_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        etCurrentPassword = view.findViewById(R.id.etCurrentPassword)
        etNewPassword = view.findViewById(R.id.etNewPassword)
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword)
        btnUpdatePassword = view.findViewById(R.id.btnUpdatePassword)
        tvLogin = view.findViewById(R.id.tvLogin)
        spinnerRole = view.findViewById(R.id.spinnerRole)

        val roles = arrayOf("Transporter", "TSD Facility")
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            roles
        )
        spinnerRole.adapter = spinnerAdapter

        btnUpdatePassword.setOnClickListener {
            val selectedRole = spinnerRole.selectedItem.toString()
            val currentPassword = etCurrentPassword.text.toString().trim()
            val newPassword = etNewPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "New passwords do not match", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (newPassword.length < 8) {
                Toast.makeText(
                    requireContext(),
                    "Password must be at least 8 characters long",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            updatePassword(currentPassword, newPassword, selectedRole)
        }

        tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_SP_ChangepasswordFragment_to_loginFragment)
        }
    }

    private fun updatePassword(currentPassword: String, newPassword: String, selectedRole: String) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val email = user.email ?: return
        val emailKey = email.replace(".", "_") // Firestore-safe key
        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        // ✅ Step 1: Get data from 'service_requests'
                        firestore.collection("service_requests")
                            .whereEqualTo("email", email)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                if (!querySnapshot.isEmpty) {
                                    val requestDoc = querySnapshot.documents[0]
                                    val name = requestDoc.getString("name") ?: ""
                                    val location = requestDoc.getString("location") ?: ""

                                    // ✅ Step 2: Save to 'service_providers'
                                    val spData = mapOf(
                                        "uid" to user.uid,
                                        "name" to name,
                                        "email" to email,
                                        "location" to location,
                                        "role" to selectedRole,
                                        "password" to newPassword,
                                        "status" to "approved",
                                        "mustChangePassword" to false,
                                        "createdAt" to com.google.firebase.Timestamp.now()
                                    )

                                    firestore.collection("service_providers").document(emailKey)
                                        .set(spData)
                                        .addOnSuccessListener {
                                            // ✅ Step 3: Optionally delete or mark the service request
                                            requestDoc.reference.update(
                                                "status",
                                                "converted_to_provider"
                                            )

                                            Toast.makeText(
                                                requireContext(),
                                                "Password updated and profile saved!",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            findNavController().navigate(R.id.action_SP_ChangepasswordFragment_to_serviceProviderDashboard)
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                requireContext(),
                                                "Error saving to service_providers: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "No matching service request found for this email.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Failed to update password: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Reauthentication failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
