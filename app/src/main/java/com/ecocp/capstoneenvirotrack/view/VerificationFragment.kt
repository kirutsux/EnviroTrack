package com.ecocp.capstoneenvirotrack.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels // Changed to activityViewModels
import androidx.navigation.fragment.findNavController
import com.ecocp.capstoneenvirotrack.R
import com.ecocp.capstoneenvirotrack.repository.UserRepository
import com.ecocp.capstoneenvirotrack.viewmodel.RegistrationViewModel
import com.ecocp.capstoneenvirotrack.viewmodel.RegistrationViewModelFactory
import com.ecocp.capstoneenvirotrack.viewmodel.UiState // Ensure you import your UiState

class VerificationFragment : Fragment() {

    private val viewModel: RegistrationViewModel by activityViewModels {
        RegistrationViewModelFactory(UserRepository()) // No Context needed
    }

    private lateinit var otp1: EditText
    private lateinit var otp2: EditText
    private lateinit var otp3: EditText
    private lateinit var otp4: EditText
    private lateinit var otp5: EditText
    private lateinit var otp6: EditText
    private lateinit var btnVerify: Button
    private lateinit var backButton: ImageView
    private lateinit var tvResendCode: TextView

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
        tvResendCode = view.findViewById(R.id.tvResend)

        setupOtpInputListeners()
        setupClickListeners()
        observeViewModel()

        return view
    }

    private fun setupOtpInputListeners() {
        val otpFields = listOf(otp1, otp2, otp3, otp4, otp5, otp6)

        for (i in otpFields.indices) {
            otpFields[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && i < otpFields.size - 1) {
                        otpFields[i + 1].requestFocus()
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            otpFields[i].setOnKeyListener { v, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (otpFields[i].text.isEmpty() && i > 0) {
                        // ✅ delete the previous field’s digit before moving back
                        otpFields[i - 1].setText("")
                        otpFields[i - 1].requestFocus()
                    }
                    true
                } else {
                    false
                }
            }
        }
    }


    private fun setupClickListeners() {
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        btnVerify.setOnClickListener {
            handleVerifyButtonClick()
        }

        tvResendCode.setOnClickListener {
            handleResendCodeClick()
        }
    }

    private fun handleVerifyButtonClick() {
        val enteredCode = "${otp1.text}${otp2.text}${otp3.text}${otp4.text}${otp5.text}${otp6.text}".trim()

        if (enteredCode.length != 6) {
            Toast.makeText(requireContext(), "Please enter the 6-digit code.", Toast.LENGTH_SHORT).show()
            return
        }

        // Retrieve arguments for context, though the ViewModel uses tempUserDetails
        val args = arguments
        val emailFromArgs = args?.getString("email") ?: ""
        Log.d("VerificationFragment", "Verify button clicked. OTP: $enteredCode. Email from args: $emailFromArgs")

        // Add logging to debug the stored verification code
        viewModel.getVerificationCodeForDebug()?.let { storedCode ->
            Log.d("VerificationFragment", "Stored verification code in ViewModel: $storedCode")
        } ?: run {
            Log.w("VerificationFragment", "No verification code stored in ViewModel!")
        }

        // Call the ViewModel's registerWithEmail method with only the enteredCode
        viewModel.registerWithEmail(
            email = emailFromArgs, // Context only, ViewModel uses tempUserDetails.email
            firstName = args?.getString("firstName") ?: "", // Context only
            lastName = args?.getString("lastName") ?: "",   // Context only
            password = args?.getString("password") ?: "",   // Context only
            phoneNumber = args?.getString("phoneNumber") ?: "", // Context only
            enteredCode = enteredCode    // Triggers the OTP verification logic
        )
    }

    private fun handleResendCodeClick() {
        Log.d("VerificationFragment", "Resend code clicked.")
        Toast.makeText(requireContext(), "Requesting a new code...", Toast.LENGTH_SHORT).show()
        viewModel.resendVerificationCodeForEmailRegistration()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state: UiState? ->
            state ?: return@observe

            // Handle Loading State (Optional: Show/Hide ProgressBar)
            // progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            state.errorMessage?.let { errorMsg ->
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                Log.e("VerificationFragment", "Error from ViewModel: $errorMsg")
                if (errorMsg == "Invalid verification code") {
                    val enteredCode = "${otp1.text}${otp2.text}${otp3.text}${otp4.text}${otp5.text}${otp6.text}".trim()
                    val storedCode = viewModel.getVerificationCodeForDebug()
                    if (storedCode != null) {
                        val firstMismatch = storedCode.zip(enteredCode).indexOfFirst { it.first != it.second }
                        when {
                            firstMismatch >= 3 -> otp4.requestFocus() // Mismatch at or after otp4
                            firstMismatch >= 2 -> otp3.requestFocus() // Mismatch at otp3
                            firstMismatch >= 1 -> otp2.requestFocus() // Mismatch at otp2
                            firstMismatch >= 0 -> otp1.requestFocus() // Mismatch at otp1
                        }
                    }
                }
                viewModel.clearErrorMessage() // Consume the error
            }

            state.successMessage?.let { successMsg ->
                if (state.navigateToDashboardForUserType == null) { // Only toast if not about to navigate
                    Toast.makeText(requireContext(), successMsg, Toast.LENGTH_SHORT).show()
                }
                Log.d("VerificationFragment", "Success from ViewModel: $successMsg")
                viewModel.clearSuccessMessage() // Consume the success message
            }

            state.navigateToDashboardForUserType?.let { userTypeOfDashboard ->
                Log.i("VerificationFragment", "Navigation signal received for user type: $userTypeOfDashboard")
                if (userTypeOfDashboard.lowercase() == "pco") {
                    Toast.makeText(requireContext(), "Verification successful! Navigating to PCO Dashboard...", Toast.LENGTH_SHORT).show()
                    try {
                        findNavController().navigate(R.id.action_verificationFragment_to_pcoDashboard)
                    } catch (e: Exception) {
                        Log.e("VerificationFragment", "Navigation to PCO Dashboard failed: ${e.message}", e)
                        Toast.makeText(requireContext(), "Error navigating to dashboard.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w("VerificationFragment", "Unexpected user type for dashboard: $userTypeOfDashboard")
                    Toast.makeText(requireContext(), "Registration successful, but dashboard type is unknown.", Toast.LENGTH_LONG).show()
                }
                viewModel.clearNavigationSignal() // Consume the navigation event
            }
        }
    }
}