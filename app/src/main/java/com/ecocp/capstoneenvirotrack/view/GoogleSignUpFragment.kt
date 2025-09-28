package com.ecocp.capstoneenvirotrack.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.repository.UserRepository
import com.ecocp.capstoneenvirotrack.viewmodel.RegistrationViewModel
import com.ecocp.capstoneenvirotrack.viewmodel.RegistrationViewModelFactory

class GoogleSignUpFragment : Fragment() {

    // Manual argument retrieval
    private val uid: String by lazy { arguments?.getString("uid") ?: "" }
    private val email: String by lazy { arguments?.getString("email") ?: "" }
    private val firstName: String by lazy { arguments?.getString("firstName") ?: "" }
    private val lastName: String by lazy { arguments?.getString("lastName") ?: "" }
    private val userType: String by lazy { arguments?.getString("userType") ?: "pco" } // Default to "pco" from navigation

    private val viewModel: RegistrationViewModel by activityViewModels {
        RegistrationViewModelFactory(UserRepository()) // No Context needed
    }

    private lateinit var etPhoneNumber: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSubmit: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_google_sign_up, container, false)

        etPhoneNumber = view.findViewById(R.id.etPhoneNumber)
        etPassword = view.findViewById(R.id.etPassword)
        btnSubmit = view.findViewById(R.id.btnSubmit)

        btnSubmit.setOnClickListener {
            val phoneNumber = etPhoneNumber.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (phoneNumber.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.completeGoogleRegistration(
                uid = uid,
                email = email,
                fullName = "$firstName $lastName", // Combine firstName and lastName
                phoneNumber = phoneNumber,
                password = password
            )
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state.isLoading) {
                // Show loading indicator
            } else {
                state.successMessage?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_googleSignUpFragment_to_embDashboard)
                }
                state.errorMessage?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                }
            }
        }

        return view
    }
}