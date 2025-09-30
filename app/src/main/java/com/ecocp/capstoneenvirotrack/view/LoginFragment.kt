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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_log_in, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val btnGoogle: Button = view.findViewById(R.id.btnGoogle)
        val btnLogin: Button = view.findViewById(R.id.btnLogin)
        val etEmail: EditText = view.findViewById(R.id.etEmail)
        val etPassword: EditText = view.findViewById(R.id.etPassword)
        val tvGoToRegister: TextView = view.findViewById(R.id.tvGoToRegister)
        val showPassword: ImageView = view.findViewById(R.id.showPassword)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

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
                        Log.d("LoginFragment", "Login successful for email: $email")
                        checkUserType(email)
                    } else {
                        Toast.makeText(requireContext(), "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        Log.e("LoginFragment", "Login failed", task.exception)
                    }
                }
        }

        tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registrationFragment)
        }

        return view
    }

    // ✅ Google Sign-In Intent with forced popup
    private fun signInWithGoogle() {
        Log.d("LoginFragment", "Google Sign-In button clicked. Forcing popup by revoking session.")

        // Sign out & revoke access so popup always appears
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInClient.revokeAccess().addOnCompleteListener {
                Log.d("LoginFragment", "Launching Google Sign-In intent...")
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }
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
                Log.e("LoginFragment", "Google Sign-In error", e)
            }
        }

    // ✅ Check if user exists in Firestore before allowing Google Sign-In
    private fun checkIfUserExists(email: String, idToken: String?) {
        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                Log.d("LoginFragment", "Google auth successful for email: $email")
                                checkUserType(email)
                            } else {
                                Toast.makeText(requireContext(), "Authentication failed!", Toast.LENGTH_SHORT).show()
                                Log.e("LoginFragment", "Google auth failed", authTask.exception)
                            }
                        }
                } else {
                    Toast.makeText(requireContext(), "Email is not registered. Please sign up first.", Toast.LENGTH_LONG).show()
                    Log.w("LoginFragment", "Email not found: $email")
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoginFragment", "Error checking user existence", e)
                Toast.makeText(requireContext(), "Error checking user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ✅ Check user type from Firestore and navigate to the correct dashboard
    private fun checkUserType(email: String) {
        Log.d("LoginFragment", "Checking user type for email: $email")

        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userDoc = documents.documents[0]
                    val userType = userDoc.getString("userType")
                    Log.d("LoginFragment", "Firestore userType: $userType")

                    if (userType != null) {
                        navigateToDashboard(userType)
                    } else {
                        Toast.makeText(requireContext(), "User type not found in Firestore!", Toast.LENGTH_SHORT).show()
                        Log.e("LoginFragment", "User type is null for email: $email")
                    }
                } else {
                    Toast.makeText(requireContext(), "User not found!", Toast.LENGTH_SHORT).show()
                    Log.w("LoginFragment", "User not found for email: $email")
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoginFragment", "Error checking user type", e)
                Toast.makeText(requireContext(), "Error checking user type: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ✅ Navigate to the correct dashboard based on Firestore userType
    private fun navigateToDashboard(userType: String) {
        when (userType) {
            "EMB", "emb" -> {
                findNavController().navigate(R.id.action_loginFragment_to_embDashboard)
            }
            "Service Provider", "service provider" -> {
                findNavController().navigate(R.id.action_loginFragment_to_serviceProviderDashboard)
            }
            "PCO", "pco" -> {
                findNavController().navigate(R.id.action_loginFragment_to_pcoDashboard)
            }
            else -> {
                Toast.makeText(requireContext(), "Invalid user type: $userType", Toast.LENGTH_SHORT).show()
                Log.w("LoginFragment", "Unknown user type: $userType")
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