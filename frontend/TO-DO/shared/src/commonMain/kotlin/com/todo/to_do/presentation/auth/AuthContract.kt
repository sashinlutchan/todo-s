package com.todo.to_do.presentation.auth

data class AuthState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val phoneNumber: String = "",
    val isRegisterMode: Boolean = false,
    val isLoading: Boolean = false
)

sealed interface AuthSideEffect {
    data object Authenticated : AuthSideEffect
    data class NeedsVerification(val email: String) : AuthSideEffect
    data class ShowError(val message: String) : AuthSideEffect
}

