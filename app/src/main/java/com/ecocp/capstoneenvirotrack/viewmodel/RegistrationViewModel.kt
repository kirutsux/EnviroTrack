package com.ecocp.capstoneenvirotrack.viewmodel

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

class RegistrationViewModel(private val repository: UserRepository) : ViewModel() {

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    /** Keeps the last verification code sent to the email */
    private var verificationCode: String? = null

    /**
     * One-tap method for manual registration:
     *  - First call (no enteredCode): sends the verification email.
     *  - Second call (with enteredCode): verifies and completes registration.
     */
    fun registerWithEmail(
        email: String,
        firstName: String,
        lastName: String,
        password: String,
        phoneNumber: String,
        userType: String,
        enteredCode: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)

            // Step 1: Send verification code if not already sent
            if (verificationCode == null) {
                val sent = withContext(Dispatchers.IO) {
                    repository.sendVerificationCode(email)
                }
                if (sent != null) {
                    verificationCode = sent
                    _uiState.value = UiState(
                        isLoading = false,
                        successMessage = "Verification code sent to $email"
                    )
                } else {
                    _uiState.value = UiState(
                        isLoading = false,
                        errorMessage = "Failed to send verification email"
                    )
                }
                return@launch
            }

            // Step 2: Verify the entered code and complete registration
            if (enteredCode.isNullOrBlank()) {
                _uiState.value = UiState(
                    isLoading = false,
                    errorMessage = "Please enter the 6-digit verification code"
                )
                return@launch
            }

            if (enteredCode != verificationCode) {
                _uiState.value = UiState(
                    isLoading = false,
                    errorMessage = "Invalid verification code"
                )
                return@launch
            }

            // Code is valid: complete registration
            val ok = repository.registerUserWithEmail(
                email = email,
                password = password,
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                userType = userType
            )

            _uiState.value = if (ok) {
                verificationCode = null // Clear code after successful use
                UiState(isLoading = false, successMessage = "Registration successful")
            } else {
                UiState(isLoading = false, errorMessage = "Registration failed")
            }
        }
    }

    fun completeGoogleRegistration(
        uid: String,
        email: String,
        fullName: String,
        phoneNumber: String,
        password: String,
        userType: String
    ) {
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)

            try {
                repository.saveUserToFirestore(
                    uid = uid,
                    email = email,
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    password = password,
                    userType = userType
                )
                _uiState.value = UiState(successMessage = "Registration successful!")
            } catch (e: Exception) {
                _uiState.value = UiState(errorMessage = e.message ?: "Failed to complete registration")
            }
        }
    }

    /** Google sign-in step 1: exchange idToken -> Firebase user */
    fun handleGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            val user = repository.signInWithGoogle(idToken)
            if (user != null) {
                _uiState.value = UiState(
                    isLoading = false,
                    successMessage = "Google sign-in successful",
                    googleUser = user
                )
            } else {
                _uiState.value = UiState(
                    isLoading = false,
                    errorMessage = "Google sign-in failed"
                )
            }
        }
    }
}

data class UiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val googleUser: FirebaseUser? = null
)

/**
 * Factory for creating instances of [RegistrationViewModel].
 */
class RegistrationViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistrationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegistrationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}