package com.ecocp.capstoneenvirotrack.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.view.serviceprovider.ServiceProvider_Dashboard
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firestore: FirebaseFirestore
    private var isPasswordVisible = false
    private var selectedUserType: String? = null // Store user type

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_log_in, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Get selected user type from SharedPreferences
        val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        selectedUserType = sharedPreferences.getString("selectedUserType", "")

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        val btnGoogle: Button = view.findViewById(R.id.btnGoogle)
        val btnLogin: Button = view.findViewById(R.id.btnLogin)
        val etEmail: EditText = view.findViewById(R.id.etEmail)
        val etPassword: EditText = view.findViewById(R.id.etPassword)
        val tvGoToRegister: TextView = view.findViewById(R.id.tvGoToRegister)
        val showPassword: ImageView = view.findViewById(R.id.showPassword)

        btnGoogle.setOnClickListener { signInWithGoogle() }

        showPassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(etPassword, showPassword, isPasswordVisible)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "All fields are required!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        checkUserType(email)
                    } else {
                        Toast.makeText(requireContext(), "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registrationFragment)
        }

        return view
    }

    // ✅ Google Sign-In Intent
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    // ✅ Handle Google Sign-In Result
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                val email = account.email

                if (email != null) {
                    checkIfUserExists(email, account.idToken)
                } else {
                    Toast.makeText(requireContext(), "Google Sign-In failed!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Google Sign-In error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

    // ✅ Check if user exists in Firestore before allowing Google Sign-In
    private fun checkIfUserExists(email: String, idToken: String?) {
        firestore.collection("Users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                checkUserType(email)
                            } else {
                                Toast.makeText(requireContext(), "Authentication failed!", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(requireContext(), "Email is not registered. Please sign up first.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error checking user existence", e)
                Toast.makeText(requireContext(), "Error checking user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ✅ Check user type before redirecting to the correct dashboard
    private fun checkUserType(email: String) {
        val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val selectedUserType = sharedPreferences.getString("selectedUserType", "")

        firestore.collection("Users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userType = documents.documents[0].getString("userType")

                    if (userType != null && userType.trim() == selectedUserType?.trim()) {
                        navigateToDashboard(userType)
                    } else {
                        Toast.makeText(requireContext(), "Invalid user type selection!", Toast.LENGTH_SHORT).show()
                        Log.e("Login", "UserType mismatch: Selected = $selectedUserType, Firestore = $userType")
                    }
                } else {
                    Toast.makeText(requireContext(), "User not found!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error checking user type!", Toast.LENGTH_SHORT).show()
            }
    }


    // ✅ Navigate to the correct dashboard
    private fun navigateToDashboard(userType: String) {
        when (userType) {
            "EMB" -> {
                findNavController().navigate(R.id.action_loginFragment_to_embDashboard)
            }
            "Service Provider" -> {
                findNavController().navigate(R.id.action_loginFragment_to_serviceProviderDashboard)
            }
            "PCO" -> {
                findNavController().navigate(R.id.action_loginFragment_to_pcoDashboard)
            }
            else -> {
                Toast.makeText(requireContext(), "Invalid user type!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // ✅ Toggle Password Visibility Function
    private fun togglePasswordVisibility(editText: EditText, toggleIcon: ImageView, isVisible: Boolean) {
        if (isVisible) {
            editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleIcon.setImageResource(R.drawable.ic_visibility_on)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleIcon.setImageResource(R.drawable.ic_visibility_off)
        }
        editText.setSelection(editText.text.length)
    }
}
