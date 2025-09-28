package com.ecocp.capstoneenvirotrack.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.* // Keep for EditText, Button, ImageView, TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.repository.UserRepository
import com.ecocp.capstoneenvirotrack.viewmodel.RegistrationViewModel
import com.ecocp.capstoneenvirotrack.viewmodel.RegistrationViewModelFactory
import com.ecocp.capstoneenvirotrack.viewmodel.UiState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class RegistrationFragment : Fragment() {

    private lateinit var googleSignInClient: GoogleSignInClient

    private val viewModel: RegistrationViewModel by activityViewModels {
        RegistrationViewModelFactory(UserRepository()) // No Context needed
    }

    private lateinit var etEmail: EditText
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var tvUserType: TextView // Reference to the PCO TextView

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                viewModel.handleGoogleSignIn(idToken)
            } ?: run {
                val errorMessage = if (account == null) "Google Sign-In failed: Account is null"
                else "Google Sign-In failed: No ID token"
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                Log.w("RegistrationFragment", errorMessage)
            }
        } catch (e: ApiException) {
            Log.w("RegistrationFragment", "Google sign in failed code: ${e.statusCode}", e)
            Toast.makeText(requireContext(), "Google Sign-In failed: ${e.message ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("RegistrationFragment", "Error processing Google Sign-In result", e)
            Toast.makeText(requireContext(), "Google Sign-In error: An unexpected error occurred.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_registration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        val btnGoogleSignUp: Button = view.findViewById(R.id.btnGoogleSignUp)
        val btnBack: ImageView = view.findViewById(R.id.btnBack)
        val btnSignUp: Button = view.findViewById(R.id.btnSignUp)
        etEmail = view.findViewById(R.id.etEmail)
        etFirstName = view.findViewById(R.id.etFirstName)
        etLastName = view.findViewById(R.id.etLastName)
        etPassword = view.findViewById(R.id.etPassword)
        etPhoneNumber = view.findViewById(R.id.etPhoneNumber)
        tvUserType = view.findViewById(R.id.UserType) // Reference the existing TextView
        val tvLoginView: TextView = view.findViewById(R.id.tvLogin)

        // Setup Click Listeners
        btnGoogleSignUp.setOnClickListener {
            Log.d("RegistrationFragment", "Google Sign-Up button clicked.")
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        btnSignUp.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val phoneNumber = etPhoneNumber.text.toString().trim()

            // Basic Input Validation
            if (email.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || password.isEmpty() || phoneNumber.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("RegistrationFragment", "Sign-Up button clicked. Calling ViewModel to register PCO with email: $email")
            viewModel.registerWithEmail(
                email = email,
                firstName = firstName,
                lastName = lastName,
                password = password,
                phoneNumber = phoneNumber,
                enteredCode = null   // Explicitly null for initial step
            )
        }

        tvLoginView.setOnClickListener {
            findNavController().navigate(R.id.action_registrationFragment_to_loginFragment)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state: UiState? ->
            state ?: return@observe // Guard against null state

            // Handle Loading State (Optional: Show/Hide ProgressBar)
            // progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            // Handle Error Message
            state.errorMessage?.let { errorMsg ->
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                Log.e("RegistrationFragment", "Error from ViewModel: $errorMsg")
                viewModel.clearErrorMessage() // Consume the error message
            }

            // Handle Success Message and Navigation
            state.successMessage?.let { successMsg ->
                try {
                    val currentNavController = findNavController()
                    when {
                        successMsg.startsWith("Verification code sent") -> {
                            Toast.makeText(requireContext(), successMsg, Toast.LENGTH_SHORT).show()
                            Log.d("RegistrationFragment", "OTP Sent. Navigating to VerificationFragment.")
                            val action =
                                RegistrationFragmentDirections.actionRegistrationFragmentToVerificationFragment(
                                    email = etEmail.text.toString().trim(),
                                    firstName = etFirstName.text.toString().trim(),
                                    lastName = etLastName.text.toString().trim(),
                                    password = etPassword.text.toString().trim(),
                                    phoneNumber = etPhoneNumber.text.toString().trim()
                                )
                            currentNavController.navigate(action)
                            viewModel.clearSuccessMessage()
                        }
                        successMsg == "Google sign-in successful" && state.googleUser != null -> {
                            Toast.makeText(requireContext(), "Google Sign-In successful. Completing setup...", Toast.LENGTH_SHORT).show()
                            val googleUser = state.googleUser
                            val fullName = googleUser.displayName ?: ""
                            val nameParts = fullName.split(" ", limit = 2)
                            val firstName = nameParts.getOrNull(0) ?: ""
                            val lastName = nameParts.getOrNull(1) ?: ""
                            val action =
                                RegistrationFragmentDirections.actionRegistrationFragmentToGoogleSignUpFragment(
                                    uid = googleUser.uid,
                                    email = googleUser.email ?: "",
                                    firstName = firstName,
                                    lastName = lastName,
                                    password = "",
                                    phoneNumber = "",
                                    userType = "pco" // Added to match the hardcoded PCO logic
                                )
                            currentNavController.navigate(action)
                            viewModel.clearSuccessMessage()
                        }
                        else -> {
                            if (successMsg.isNotEmpty()) {
                                Toast.makeText(requireContext(), successMsg, Toast.LENGTH_SHORT).show()
                            }
                            viewModel.clearSuccessMessage()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("RegistrationFragment", "Navigation failed: ${e.message}", e)
                    Toast.makeText(requireContext(), "Navigation error. Please try again.", Toast.LENGTH_LONG).show()
                    viewModel.clearSuccessMessage()
                }
            }
        }
    }
}