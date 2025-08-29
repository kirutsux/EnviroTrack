package com.ecocp.capstoneenvirotrack.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.repository.UserRepository
import com.ecocp.capstoneenvirotrack.viewmodel.RegistrationViewModel
import com.ecocp.capstoneenvirotrack.viewmodel.RegistrationViewModelFactory

class VerificationFragment : Fragment() {

    private val viewModel: RegistrationViewModel by viewModels {
        RegistrationViewModelFactory(UserRepository())
    }

    private lateinit var otp1: EditText
    private lateinit var otp2: EditText
    private lateinit var otp3: EditText
    private lateinit var otp4: EditText
    private lateinit var otp5: EditText
    private lateinit var otp6: EditText
    private lateinit var btnVerify: Button
    private lateinit var backButton: ImageView
    private lateinit var tvResend: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_verification, container, false)

        otp1 = view.findViewById(R.id.otp1)
        otp2 = view.findViewById(R.id.otp2)
        otp3 = view.findViewById(R.id.otp3)
        otp4 = view.findViewById(R.id.otp4)
        otp5 = view.findViewById(R.id.otp5)
        otp6 = view.findViewById(R.id.otp6)
        btnVerify = view.findViewById(R.id.btnVerify)
        backButton = view.findViewById(R.id.backButton)
        tvResend = view.findViewById(R.id.tvResend)

        backButton.setOnClickListener { findNavController().popBackStack() }

        btnVerify.setOnClickListener {
            val enteredCode = "${otp1.text}${otp2.text}${otp3.text}${otp4.text}${otp5.text}${otp6.text}".trim()
            if (enteredCode.length != 6) {
                Toast.makeText(requireContext(), "Please enter a 6-digit code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val args = arguments
            val email = args?.getString("email") ?: ""
            val firstName = args?.getString("firstName") ?: ""
            val lastName = args?.getString("lastName") ?: ""
            val password = args?.getString("password") ?: ""
            val phoneNumber = args?.getString("phoneNumber") ?: ""
            val userType = args?.getString("userType")?.lowercase() ?: ""

            viewModel.registerWithEmail(email, firstName, lastName, password, phoneNumber, userType, enteredCode)

            viewModel.uiState.observe(viewLifecycleOwner) { state ->
                if (state.isLoading) {
                    return@observe
                }

                state.errorMessage?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                }

                state.successMessage?.let { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    if (msg == "Registration successful") {
                        when (userType) {
                            "service provider" -> findNavController().navigate(R.id.action_verificationFragment_to_serviceProviderDashboard)
                            "emb" -> findNavController().navigate(R.id.action_verificationFragment_to_embDashboard)
                            "pco" -> findNavController().navigate(R.id.action_verificationFragment_to_pcoDashboard)
                            else -> Toast.makeText(requireContext(), "Unknown user type", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        tvResend.setOnClickListener {
            val email = arguments?.getString("email") ?: ""
            if (email.isNotEmpty()) {
                viewModel.registerWithEmail(email, "", "", "", "", "", null)
                Toast.makeText(requireContext(), "Verification code resent to $email", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Email not available", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}