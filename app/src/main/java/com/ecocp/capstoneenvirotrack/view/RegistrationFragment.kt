package com.ecocp.capstoneenvirotrack.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.repository.UserRepository
import com.ecocp.capstoneenvirotrack.viewmodel.RegistrationViewModel
import com.ecocp.capstoneenvirotrack.viewmodel.RegistrationViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class RegistrationFragment : Fragment() {

    private lateinit var googleSignInClient: GoogleSignInClient

    private val viewModel: RegistrationViewModel by viewModels {
        RegistrationViewModelFactory(UserRepository())
    }

    private lateinit var etEmail: EditText
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var spUserType: Spinner
    private lateinit var tvLogin: TextView

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        val account = task.result
        account?.idToken?.let { idToken ->
            viewModel.handleGoogleSignIn(idToken)
        } ?: run {
            Toast.makeText(requireContext(), "Google Sign-In failed: No ID token", Toast.LENGTH_SHORT).show()
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
        val view = inflater.inflate(R.layout.fragment_registration, container, false)

        val btnGoogleSignUp: Button = view.findViewById(R.id.btnGoogleSignUp)
        val btnBack: ImageView = view.findViewById(R.id.btnBack)
        val btnSignUp: Button = view.findViewById(R.id.btnSignUp)
        etEmail = view.findViewById(R.id.etEmail)
        etFirstName = view.findViewById(R.id.etFirstName)
        etLastName = view.findViewById(R.id.etLastName)
        etPassword = view.findViewById(R.id.etPassword)
        etPhoneNumber = view.findViewById(R.id.etPhoneNumber)
        spUserType = view.findViewById(R.id.spUserType)
        tvLogin = view.findViewById(R.id.tvLogin)

        // Populate Spinner with register_as_options from strings.xml
        spUserType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.register_as_options)
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        btnGoogleSignUp.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        btnSignUp.setOnClickListener {
            viewModel.registerWithEmail(
                email = etEmail.text.toString().trim(),
                firstName = etFirstName.text.toString().trim(),
                lastName = etLastName.text.toString().trim(),
                password = etPassword.text.toString().trim(),
                phoneNumber = etPhoneNumber.text.toString().trim(),
                userType = spUserType.selectedItem?.toString() ?: ""
            )
        }

        tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registrationFragment_to_loginFragment)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state.isLoading) {
                // TODO: Show loading indicator
                return@observe
            }

            state.errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }

            state.successMessage?.let { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

                if (view != null && isAdded) {
                    try {
                        val navController = try {
                            findNavController()
                        } catch (e: IllegalStateException) {
                            Log.w("RegistrationFragment", "Fragment NavController failed, using activity fallback")
                            requireActivity().findNavController(R.id.nav_host_fragment) // No change needed if ID is correct
                        }
                        when {
                            msg.startsWith("Verification code sent") -> {
                                val action = RegistrationFragmentDirections.actionRegistrationFragmentToVerificationFragment(
                                    email = etEmail.text.toString().trim(),
                                    firstName = etFirstName.text.toString().trim(),
                                    lastName = etLastName.text.toString().trim(),
                                    password = etPassword.text.toString().trim(),
                                    phoneNumber = etPhoneNumber.text.toString().trim(),
                                    userType = spUserType.selectedItem?.toString() ?: ""
                                )
                                navController.navigate(action)
                                Log.d("RegistrationFragment", "Navigated to VerificationFragment")
                            }
                            msg == "Google sign-in successful" -> {
                                val fullName = state.googleUser?.displayName ?: ""
                                val nameParts = fullName.split(" ", limit = 2)
                                val firstName = nameParts.getOrNull(0) ?: ""
                                val lastName = nameParts.getOrNull(1) ?: ""
                                val action = RegistrationFragmentDirections.actionRegistrationFragmentToGoogleSignUpFragment(
                                    uid = state.googleUser?.uid ?: "",
                                    email = state.googleUser?.email ?: "",
                                    firstName = firstName,
                                    lastName = lastName,
                                    password = "",
                                    phoneNumber = "",
                                    userType = ""
                                )
                                navController.navigate(action)
                                Log.d("RegistrationFragment", "Navigated to GoogleSignUpFragment")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("RegistrationFragment", "Navigation failed: ${e.message}", e)
                        Toast.makeText(requireContext(), "Navigation failed, please try again", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        return view
    }
}