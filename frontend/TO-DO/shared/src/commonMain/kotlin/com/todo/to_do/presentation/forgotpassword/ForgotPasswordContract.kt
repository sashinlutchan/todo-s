package com.todo.to_do.presentation.forgotpassword

data class ForgotPasswordState(
    val email: String = "",
    val isLoading: Boolean = false
)

sealed interface ForgotPasswordSideEffect {
    data class CodeSent(val email: String) : ForgotPasswordSideEffect
    data class ShowError(val message: String) : ForgotPasswordSideEffect
}

