package com.example.datagov.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        val currentUser = auth.currentUser
        _authState.value = AuthState(
            user = currentUser,
            isAuthenticated = currentUser != null
        )
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState(
                    user = result.user,
                    isAuthenticated = result.user != null,
                    isLoading = false
                )
                Log.d("AuthViewModel", "Sign in successful: ${result.user?.email}")
            } catch (e: Exception) {
                _authState.value = AuthState(
                    error = getErrorMessage(e),
                    isLoading = false
                )
                Log.e("AuthViewModel", "Sign in failed", e)
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                _authState.value = AuthState(
                    user = result.user,
                    isAuthenticated = result.user != null,
                    isLoading = false
                )
                Log.d("AuthViewModel", "Sign up successful: ${result.user?.email}")
            } catch (e: Exception) {
                _authState.value = AuthState(
                    error = getErrorMessage(e),
                    isLoading = false
                )
                Log.e("AuthViewModel", "Sign up failed", e)
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState(isAuthenticated = false)
        Log.d("AuthViewModel", "User signed out")
    }

    private fun getErrorMessage(exception: Exception): String {
        return when {
            exception.message?.contains("password") == true -> "Contraseña incorrecta"
            exception.message?.contains("email") == true -> "Email inválido"
            exception.message?.contains("user") == true -> "Usuario no encontrado"
            exception.message?.contains("network") == true -> "Error de conexión"
            else -> exception.message ?: "Error desconocido"
        }
    }
}

