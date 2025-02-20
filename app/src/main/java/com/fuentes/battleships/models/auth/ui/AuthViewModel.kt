package com.fuentes.battleships.models.auth.ui

import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuthState())
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email == "notes@test.com" && password == "123") {
            _uiState.update { currentState ->
                currentState.copy(
                    email = email
                )
            }
        }
    }

    fun logout() {
        _uiState.update { currentState ->
            currentState.copy(
                email = null
            )
        }
    }
}