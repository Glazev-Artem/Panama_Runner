package com.glazev.panama_runner.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glazev.panama_runner.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val isUserSignedIn: StateFlow<Boolean> = authRepository.isUserSignedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userEmail: String?
        get() = authRepository.currentUserEmail

    fun onSignInResult(idToken: String) {
        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
