package com.ecocp.capstoneenvirotrack.view.serviceprovider

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import com.google.firebase.firestore.SetOptions



class SP_ChangepasswordFragment : Fragment() {

    private lateinit var etName: EditText
    private lateinit var etCompanyName: EditText
    private lateinit var etContactNumber: EditText
    private lateinit var etEmail: EditText
    private lateinit var etAddress: EditText
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

        etName = view.findViewById(R.id.etName)
        etCompanyName = view.findViewById(R.id.etCompanyName)
        etContactNumber = view.findViewById(R.id.etContactNumber)
        etEmail = view.findViewById(R.id.etEmail)
        etAddress = view.findViewById(R.id.etAddress)
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

        // Pre-fill email if logged in
        auth.currentUser?.email?.let { etEmail.setText(it) }

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
            val intent = Intent(requireContext(), SPMainActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun updatePassword(currentPassword: String, newPassword: String, selectedRole: String) {
        val user = auth.currentUser ?: run {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val email = user.email ?: return
        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {

                        val spData = mapOf(
                            "uid" to user.uid,
                            "name" to etName.text.toString().trim(),
                            "companyName" to etCompanyName.text.toString().trim(),
                            "contactNumber" to etContactNumber.text.toString().trim(),
                            "email" to email,
                            "location" to etAddress.text.toString().trim(),
                            "role" to selectedRole,
                            "password" to newPassword,
                            "status" to "approved",
                            "mustChangePassword" to false,
                            "lastUpdated" to Timestamp.now()
                        )

                        firestore.collection("service_providers").document(user.uid)
                            .set(spData, SetOptions.merge())
                            .addOnSuccessListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Password updated and profile saved!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                findNavController().navigate(R.id.action_SP_ChangepasswordFragment_to_serviceProviderDashboard)

                                val uid = user.uid

                                // Fetch existing data first to preserve profileImageUrl and createdAt
                                firestore.collection("service_providers").document(uid)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        val existingProfileUrl =
                                            document.getString("profileImageUrl")
                                        val existingCreatedAt =
                                            document.getTimestamp("createdAt") ?: Timestamp.now()

                                        val spData = mapOf(
                                            "uid" to uid,
                                            "name" to etName.text.toString().trim(),
                                            "companyName" to etCompanyName.text.toString().trim(),
                                            "contactNumber" to etContactNumber.text.toString()
                                                .trim(),
                                            "email" to email,
                                            "location" to etAddress.text.toString().trim(),
                                            "role" to selectedRole,
                                            "status" to "approved",
                                            "password" to newPassword,
                                            "mustChangePassword" to false,
                                            "profileImageUrl" to existingProfileUrl,
                                            "createdAt" to existingCreatedAt
                                        )

                                        firestore.collection("service_providers").document(uid)
                                            .set(spData)
                                            .addOnSuccessListener {
                                                Toast.makeText(
                                                    requireContext(),
                                                    "Password changed successfully!",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                // ✅ Save login session
                                                val prefs = requireContext().getSharedPreferences(
                                                    "user_prefs",
                                                    Context.MODE_PRIVATE
                                                )
                                                prefs.edit()
                                                    .putBoolean("isLoggedIn", true)
                                                    .putString("userType", "service_provider")
                                                    .apply()

                                                // ✅ Redirect to SP Dashboard
                                                val intent = Intent(
                                                    requireContext(),
                                                    SPMainActivity::class.java
                                                )
                                                intent.addFlags(
                                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                )
                                                startActivity(intent)
                                                requireActivity().finish()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(
                                                    requireContext(),
                                                    "Failed to update Firestore: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }

                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(
                                            requireContext(),
                                            "Error fetching profile: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
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
}
