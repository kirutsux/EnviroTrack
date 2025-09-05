package com.ecocp.capstoneenvirotrack.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
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
    private val fullName: String by lazy { arguments?.getString("fullName") ?: "" }

    private val viewModel: RegistrationViewModel by viewModels {
        RegistrationViewModelFactory(UserRepository())
    }

    private lateinit var etPhoneNumber: EditText
    private lateinit var etPassword: EditText
    private lateinit var spUserType: Spinner
    private lateinit var btnSubmit: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_google_sign_up, container, false)

        etPhoneNumber = view.findViewById(R.id.etPhoneNumber)
        etPassword = view.findViewById(R.id.etPassword)
        spUserType = view.findViewById(R.id.spUserType)
        btnSubmit = view.findViewById(R.id.btnSubmit)

        // Populate Spinner with user types
        val userTypes = arrayOf("emb", "service provider", "pco")
        spUserType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            userTypes
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        btnSubmit.setOnClickListener {
            viewModel.completeGoogleRegistration(
                uid = uid,
                email = email,
                fullName = fullName,
                phoneNumber = etPhoneNumber.text.toString().trim(),
                password = etPassword.text.toString().trim(),
                userType = spUserType.selectedItem?.toString() ?: ""
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