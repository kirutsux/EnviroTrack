package com.ecocp.capstoneenvirotrack.view.all

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.view.serviceprovider.SPMainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

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
        val tvRequestAccess: TextView = view.findViewById(R.id.tvRegister)
        tvRequestAccess.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_SP_RegistrationFragment)
        }

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
                        saveFcmTokenForCurrentUser()
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
                                saveFcmTokenForCurrentUser()
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

    private fun saveFcmTokenForCurrentUser() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val userRef = FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)

                // Use arrayUnion to store multiple tokens without duplicates
                userRef.update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayUnion(token))
                    .addOnSuccessListener {
                        Log.d("LoginFragment", "FCM token added to fcmTokens array successfully")
                    }
                    .addOnFailureListener { e ->
                        // If the document doesn't exist, create it with fcmTokens array
                        userRef.set(mapOf("fcmTokens" to listOf(token)), com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener {
                                Log.d("LoginFragment", "FCM token created successfully for new user document")
                            }
                            .addOnFailureListener { ex ->
                                Log.e("LoginFragment", "Error creating FCM token array", ex)
                            }

                        Log.e("LoginFragment", "Error updating FCM token array", e)
                    }
            } else {
                Log.e("LoginFragment", "Failed to get FCM token", task.exception)
            }
        }
    }


    private fun saveUserTypeToPrefs(userType: String) {
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("userType", userType.lowercase()).apply()
    }

    // ✅ Check user type from Firestore and navigate to the correct dashboard
    // ✅ Check user type from Firestore and navigate accordingly
    private fun checkUserType(email: String) {
        val firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(requireContext(), "No authenticated user found.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1️⃣ Check if user exists in `users` first
        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { userDocs ->
                if (!userDocs.isEmpty) {
                    val userDoc = userDocs.documents[0]
                    val userType = userDoc.getString("userType")?.lowercase()

                    Log.d("LoginFragment", "User found in users: $email ($userType)")

                    when (userType) {
                        "emb" -> {
                            saveUserTypeToPrefs("emb")
                            findNavController().navigate(R.id.action_loginFragment_to_embDashboard)
                        }
                        "pco" -> {
                            saveUserTypeToPrefs("pco")
                            findNavController().navigate(R.id.action_loginFragment_to_pcoDashboard)
                        }
                        "service_provider" -> {
                            saveUserTypeToPrefs("service_provider")
                            findNavController().navigate(R.id.action_loginFragment_to_serviceProviderDashboard)
                        }
                    }

                } else {
                    // 2️⃣ Not found in users → check service_providers
                    firestore.collection("service_providers")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { spDocs ->
                            if (!spDocs.isEmpty) {
                                val spDoc = spDocs.documents[0]
                                val status = spDoc.getString("status") ?: "pending"
                                val mustChangePassword = spDoc.getBoolean("mustChangePassword") ?: true

                                Log.d(
                                    "LoginFragment",
                                    "Service Provider found: $email, status=$status, mustChangePassword=$mustChangePassword"
                                )

                                if (status != "approved") {
                                    Toast.makeText(
                                        requireContext(),
                                        "Your account is not yet approved.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@addOnSuccessListener
                                }

                                if (mustChangePassword) {
                                    findNavController().navigate(R.id.action_loginFragment_to_SP_ChangepasswordFragment)
                                } else {
                                    // ✅ Save login session for SP user
                                    val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                    prefs.edit()
                                        .putBoolean("isLoggedIn", true)
                                        .putString("userType", "service_provider")
                                        .apply()

                                    // ✅ Redirect to SP dashboard
                                    val intent = Intent(requireContext(), SPMainActivity::class.java)
                                    startActivity(intent)
                                    requireActivity().finish()
                                }

                            } else {
                                // 3️⃣ Not found → check if there's an approved service request for this email
                                firestore.collection("service_requests")
                                    .whereEqualTo("email", email)
                                    .whereEqualTo("status", "approved")
                                    .get()
                                    .addOnSuccessListener { reqDocs ->
                                        if (!reqDocs.isEmpty) {
                                            // ✅ Auto-create service_provider record
                                            val uid = currentUser.uid
                                            val newProvider = hashMapOf(
                                                "email" to email,
                                                "status" to "approved",
                                                "mustChangePassword" to true,
                                                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                            )

                                            firestore.collection("service_providers")
                                                .document(uid)
                                                .set(newProvider)
                                                .addOnSuccessListener {
                                                    Log.d("LoginFragment", "Auto-created service provider for $email")
                                                    Toast.makeText(
                                                        requireContext(),
                                                        "Welcome! Please change your password to complete setup.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    findNavController().navigate(R.id.action_loginFragment_to_SP_ChangepasswordFragment)
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("LoginFragment", "Error creating service provider", e)
                                                    Toast.makeText(
                                                        requireContext(),
                                                        "Error saving provider data.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        } else {
                                            Toast.makeText(
                                                requireContext(),
                                                "No account found with this email.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            Log.w("LoginFragment", "No user, service provider, or approved service request found for $email")
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("LoginFragment", "Error checking service_requests", e)
                                        Toast.makeText(
                                            requireContext(),
                                            "Error checking requests: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                requireContext(),
                                "Error checking service provider: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error checking users: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
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