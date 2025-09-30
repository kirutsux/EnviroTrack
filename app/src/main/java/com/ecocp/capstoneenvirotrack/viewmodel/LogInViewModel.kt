package com.ecocp.capstoneenvirotrack.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ecocp.capstoneenvirotrack.repository.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: UserRepository) : ViewModel() {

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> get() = _uiState

    data class UiState(
        val isLoading: Boolean = false,
        val successMessage: String? = null,
        val errorMessage: String? = null,
        val googleUser: FirebaseUser? = null
    )

    fun signInWithEmail(
        email: String,
        password: String,
        phoneNumber: String,
        userType: String,
        firstName: String,
        lastName: String
    ) {
        _uiState.value = UiState(isLoading = true)
        viewModelScope.launch {
            try {
                val result = repository.signInWithEmailAndPassword(email, password)
                if (result != null) {
                    // Save/update user info in Firestore
                    repository.saveUserToFirestore(
                        uid = result.uid,
                        email = email,
                        firstName = firstName,
                        lastName = lastName,
                        phoneNumber = phoneNumber,
                        password = password,
                        userType = userType
                    )
                    _uiState.value = UiState(isLoading = false, successMessage = "Login Successful!")
                } else {
                    _uiState.value = UiState(isLoading = false, errorMessage = "Login failed")
                }
            } catch (e: Exception) {
                _uiState.value = UiState(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun signInWithGoogle(account: GoogleSignInAccount) {
        _uiState.value = UiState(isLoading = true)
        viewModelScope.launch {
            try {
                val idToken = account.idToken
                if (idToken != null) {
                    val user = repository.signInWithGoogle(idToken)
                    if (user != null) {
                        // Split display name into firstName + lastName
                        val displayName = account.displayName ?: ""
                        val nameParts = displayName.trim().split(" ")
                        val firstName = nameParts.firstOrNull() ?: ""
                        val lastName =
                            if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else ""

                        // Save to Firestore (Google users are PCO by default)
                        repository.saveUserToFirestore(
                            uid = user.uid,
                            email = account.email ?: "",
                            firstName = firstName,
                            lastName = lastName,
                            phoneNumber = "", // they can update in account settings
                            password = "", // not needed for Google sign-in
                            userType = "pco"
                        )

                        _uiState.value = UiState(
                            isLoading = false,
                            successMessage = "Google Sign-In Successful!",
                            googleUser = user
                        )
                    } else {
                        _uiState.value =
                            UiState(isLoading = false, errorMessage = "Google Sign-In failed")
                    }
                } else {
                    _uiState.value = UiState(isLoading = false, errorMessage = "No ID token available")
                }
            } catch (e: Exception) {
                _uiState.value = UiState(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun handleGoogleSignInResult(account: GoogleSignInAccount?) {
        account?.let {
            signInWithGoogle(it)
        } ?: run {
            _uiState.value = UiState(errorMessage = "No Google account available")
        }
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(UserRepository()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
