package com.ecocp.capstoneenvirotrack.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ecocp.capstoneenvirotrack.repository.UserRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// UiState data class
data class UiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val googleUser: FirebaseUser? = null,
    val sessionExpired: Boolean = false,
    val navigateToDashboardForUserType: String? = null // Always "pco" for registration
)

class RegistrationViewModel(private val repository: UserRepository) : ViewModel() {

    private val _uiState = MutableLiveData(UiState()) // Initialize with default state
    val uiState: LiveData<UiState> = _uiState

    /** Keeps the last verification code sent to the email and its timestamp */
    private var verificationCode: String? = null
    private var codeSentTime: Long? = null // Timestamp when the code was sent
    private val SESSION_TIMEOUT_MINUTES = 10 // Verification code valid for 10 minutes
    private var tempUserDetails: UserDetails? = null // Temporary storage for user details

    // Data class to hold temporary user details
    data class UserDetails(
        val email: String,
        val firstName: String,
        val lastName: String,
        val password: String,
        val phoneNumber: String
    )

    /**
     * Stores user details temporarily for the PCO registration process.
     */
    fun storeUserDetails(
        email: String,
        firstName: String,
        lastName: String,
        password: String,
        phoneNumber: String
    ) {
        tempUserDetails = UserDetails(email, firstName, lastName, password, phoneNumber)
        Log.d("RegistrationViewModel", "Stored user details: Email=$email, FirstName=$firstName")
    }

    /**
     * One-tap method for PCO registration:
     *  - First call (no enteredCode): sends OTP.
     *  - Second call (with enteredCode): verifies and completes registration.
     */
    fun registerWithEmail(
        email: String,
        firstName: String,
        lastName: String,
        password: String,
        phoneNumber: String,
        enteredCode: String? = null
    ) {
        viewModelScope.launch {
            Log.d("RegistrationViewModel", "registerWithEmail called. enteredCode=$enteredCode, tempUserDetails=$tempUserDetails, verificationCode=$verificationCode")
            // Set loading state and clear previous messages
            _uiState.value = _uiState.value?.copy(
                isLoading = true,
                successMessage = null,
                errorMessage = null,
                sessionExpired = false,
                navigateToDashboardForUserType = null
            ) ?: UiState(isLoading = true)

            // Check session expiration only if an enteredCode is provided
            if (enteredCode != null && codeSentTime != null) {
                val currentTime = Calendar.getInstance().timeInMillis
                val timeElapsed = (currentTime - codeSentTime!!) / (1000 * 60) // Minutes
                if (timeElapsed > SESSION_TIMEOUT_MINUTES) {
                    verificationCode = null
                    codeSentTime = null
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        errorMessage = "Registration session expired. Please restart registration.",
                        sessionExpired = true
                    )
                    Log.w("RegistrationViewModel", "Session expired. Reset verificationCode.")
                    return@launch
                }
            }

            // Step 1: Handle initial call (no enteredCode) - send OTP
            if (enteredCode == null) {
                storeUserDetails(email, firstName, lastName, password, phoneNumber)
                val sentCode = withContext(Dispatchers.IO) {
                    repository.sendVerificationCode(email)
                }
                if (sentCode != null) {
                    verificationCode = sentCode
                    codeSentTime = Calendar.getInstance().timeInMillis
                    Log.d("RegistrationViewModel", "Sent verification code: $sentCode")
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        successMessage = "Verification code sent to $email"
                    )
                } else {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        errorMessage = "Failed to send verification email"
                    )
                    Log.e("RegistrationViewModel", "Failed to send verification code for email: $email")
                }
                return@launch
            }

            // Step 2: Verify OTP and complete PCO registration
            if (enteredCode.isNullOrBlank()) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = "Please enter the 6-digit verification code"
                )
                return@launch
            }

            if (verificationCode == null) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = "No verification code available. Please resend or restart registration."
                )
                Log.e("RegistrationViewModel", "Verification failed: No stored verification code.")
                return@launch
            }

            if (enteredCode != verificationCode) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = "Invalid verification code"
                )
                Log.w("RegistrationViewModel", "Verification failed: Entered=$enteredCode, Stored=$verificationCode")
                return@launch
            }

            // Code is valid: complete PCO registration
            val userDetails = tempUserDetails ?: run {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = "User details not found. Please restart registration."
                )
                Log.e("RegistrationViewModel", "No tempUserDetails available.")
                return@launch
            }

            try {
                val registrationSuccessful = withContext(Dispatchers.IO) {
                    repository.registerUserWithEmail(
                        email = userDetails.email,
                        password = userDetails.password,
                        firstName = userDetails.firstName,
                        lastName = userDetails.lastName,
                        phoneNumber = userDetails.phoneNumber,
                        userType = "pco" // Hardcoded to PCO for all registrations
                    )
                }

                if (registrationSuccessful) {
                    verificationCode = null
                    codeSentTime = null
                    tempUserDetails = null // Clear stored details
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        successMessage = "Registration successful",
                        navigateToDashboardForUserType = "pco"
                    )
                    Log.d("RegistrationViewModel", "Registration successful for email: ${userDetails.email}")
                } else {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        errorMessage = "Registration failed"
                    )
                    Log.e("RegistrationViewModel", "Registration failed for email: ${userDetails.email}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "An unknown registration error occurred"
                )
                Log.e("RegistrationViewModel", "Registration error: ${e.message}", e)
            }
        }
    }

    /**
     * Resends the verification code using the stored email.
     */
    fun resendVerificationCodeForEmailRegistration() {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(
                isLoading = true,
                successMessage = null,
                errorMessage = null,
                sessionExpired = false
            ) ?: UiState(isLoading = true)

            val userDetails = tempUserDetails ?: run {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = "No user details found. Please restart registration."
                )
                Log.e("RegistrationViewModel", "No tempUserDetails for resend.")
                return@launch
            }

            val sentCode = withContext(Dispatchers.IO) {
                repository.sendVerificationCode(userDetails.email)
            }
            if (sentCode != null) {
                verificationCode = sentCode
                codeSentTime = Calendar.getInstance().timeInMillis
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    successMessage = "Verification code resent to ${userDetails.email}"
                )
                Log.d("RegistrationViewModel", "Resent verification code: $sentCode")
            } else {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = "Failed to resend verification email"
                )
                Log.e("RegistrationViewModel", "Failed to resend verification code for email: ${userDetails.email}")
            }
        }
    }

    /**
     * Clears the navigation signal to prevent multiple navigations.
     */
    fun clearNavigationSignal() {
        _uiState.value = _uiState.value?.copy(navigateToDashboardForUserType = null)
    }

    fun completeGoogleRegistration(
        uid: String,
        email: String,
        fullName: String,
        phoneNumber: String,
        password: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoading = true, successMessage = null, errorMessage = null)
                ?: UiState(isLoading = true)

            try {
                withContext(Dispatchers.IO) {
                    repository.saveUserToFirestore(
                        uid = uid,
                        email = email,
                        fullName = fullName,
                        phoneNumber = phoneNumber,
                        password = password,
                        userType = "pco" // Hardcoded to PCO for Google registrations
                    )
                }
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    successMessage = "Registration successful!",
                    navigateToDashboardForUserType = "pco"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to complete registration"
                )
            }
        }
    }

    fun handleGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoading = true, successMessage = null, errorMessage = null)
                ?: UiState(isLoading = true)

            val user = try {
                withContext(Dispatchers.IO) {
                    repository.signInWithGoogle(idToken)
                }
            } catch (e: Exception) {
                null
            }

            if (user != null) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    successMessage = "Google sign-in successful",
                    googleUser = user
                )
            } else {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = "Google sign-in failed"
                )
            }
        }
    }

    fun clearErrorMessage() {
        if (_uiState.value?.errorMessage != null) {
            _uiState.value = _uiState.value?.copy(errorMessage = null)
        }
    }

    fun clearSuccessMessage() {
        if (_uiState.value?.successMessage != null) {
            _uiState.value = _uiState.value?.copy(successMessage = null)
        }
    }

    // Added method to access verificationCode for debugging
    fun getVerificationCodeForDebug(): String? {
        return verificationCode
    }
}

class RegistrationViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistrationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegistrationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}