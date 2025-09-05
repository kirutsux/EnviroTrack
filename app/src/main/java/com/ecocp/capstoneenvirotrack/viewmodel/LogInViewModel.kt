package com.ecocp.capstoneenvirotrack.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ecocp.capstoneenvirotrack.model.User
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

    fun signInWithEmail(email: String, password: String, phoneNumber: String, userType: String) {
        _uiState.value = UiState(isLoading = true)
        viewModelScope.launch {
            try {
                val result = repository.signInWithEmailAndPassword(email, password)
                if (result != null) {
                    // Optionally update Firestore with additional details if needed
                    repository.saveUserToFirestore(
                        uid = result.uid,
                        email = email,
                        fullName = "", // Placeholder, update if needed
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

    fun signInWithGoogle(idToken: String) {
        _uiState.value = UiState(isLoading = true)
        viewModelScope.launch {
            val user = repository.signInWithGoogle(idToken)
            if (user != null) {
                _uiState.value = UiState(isLoading = false, successMessage = "Google Sign-In Successful!", googleUser = user)
            } else {
                _uiState.value = UiState(isLoading = false, errorMessage = "Google Sign-In failed")
            }
        }
    }

    fun handleGoogleSignInResult(account: GoogleSignInAccount?) {
        account?.idToken?.let { idToken ->
            signInWithGoogle(idToken)
        } ?: run {
            _uiState.value = UiState(errorMessage = "No ID token available")
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