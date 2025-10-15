package com.ecocp.capstoneenvirotrack.view.all

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
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
import androidx.fragment.app.activityViewModels
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
        RegistrationViewModelFactory(UserRepository())
    }

    private lateinit var etEmail: EditText
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var tvUserType: TextView

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("RegistrationFragment", "Google Sign-In intent returned, resultCode=${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    Log.d("RegistrationFragment", "Google account retrieved: email=${account.email}, displayName=${account.displayName}")
                    if (account.idToken != null) {
                        Log.d("RegistrationFragment", "Google ID Token retrieved. Sending to ViewModel...")
                        viewModel.handleGoogleSignIn(account.idToken!!)
                    } else {
                        Log.e("RegistrationFragment", "Google ID token is null")
                        Toast.makeText(requireContext(), "Google Sign-In failed: No ID token", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("RegistrationFragment", "Google account is null")
                    Toast.makeText(requireContext(), "Google Sign-In failed: Null account", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e("RegistrationFragment", "Google sign in failed: ${e.statusCode}", e)
                Toast.makeText(requireContext(), "Google Sign-In failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("RegistrationFragment", "Unexpected error in Google Sign-In result", e)
                Toast.makeText(requireContext(), "Unexpected error occurred during Google Sign-In.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.w("RegistrationFragment", "Google Sign-In canceled or no data returned")
            Toast.makeText(requireContext(), "Google Sign-In canceled.", Toast.LENGTH_SHORT).show()
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

        val btnGoogleSignUp: Button = view.findViewById(R.id.btnGoogleSignUp)
        val btnBack: ImageView = view.findViewById(R.id.btnBack)
        val btnSignUp: Button = view.findViewById(R.id.btnSignUp)
        etEmail = view.findViewById(R.id.etEmail)
        etFirstName = view.findViewById(R.id.etFirstName)
        etLastName = view.findViewById(R.id.etLastName)
        etPassword = view.findViewById(R.id.etPassword)
        etPhoneNumber = view.findViewById(R.id.etPhoneNumber)
        tvUserType = view.findViewById(R.id.UserType)
        val tvLoginView: TextView = view.findViewById(R.id.tvLogin)

        btnGoogleSignUp.setOnClickListener {
            Log.d("RegistrationFragment", "Google Sign-Up button clicked. Forcing popup by revoking session.")
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInClient.revokeAccess().addOnCompleteListener {
                    Log.d("RegistrationFragment", "Launching Google Sign-In intent...")
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                }
            }
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

            if (email.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || password.isEmpty() || phoneNumber.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("RegistrationFragment", "Manual Sign-Up clicked. Registering PCO with email: $email")
            viewModel.registerWithEmail(
                email = email,
                firstName = firstName,
                lastName = lastName,
                password = password,
                phoneNumber = phoneNumber,
                enteredCode = null
            )
        }

        tvLoginView.setOnClickListener {
            findNavController().navigate(R.id.action_registrationFragment_to_loginFragment)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state: UiState? ->
            state ?: return@observe

            state.errorMessage?.let { errorMsg ->
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                Log.e("RegistrationFragment", "Error from ViewModel: $errorMsg")
                viewModel.clearErrorMessage()
            }

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
                        // ✅ Google sign-in case handled here
                        state.googleUser != null && state.navigateToDashboardForUserType == "pco" -> {
                            val googleUser = state.googleUser
                            Log.d("RegistrationFragment", "Google Sign-In completed. UID=${googleUser.uid}, email=${googleUser.email}")
                            Toast.makeText(requireContext(), "Google Sign-In successful. Registered as PCO.", Toast.LENGTH_SHORT).show()
                            currentNavController.navigate(R.id.action_registrationFragment_to_dashboardFragment)
                            viewModel.clearNavigationSignal()
                            viewModel.clearSuccessMessage()
                        }
                        else -> {
                            if (successMsg.isNotEmpty()) {
                                Toast.makeText(requireContext(), successMsg, Toast.LENGTH_SHORT).show()
                                Log.d("RegistrationFragment", "Other success: $successMsg")
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