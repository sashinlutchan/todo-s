package com.todo.to_do.presentation.verifyemail

data class VerifyEmailState(
    val code: String = "",
    val isLoading: Boolean = false
)

sealed interface VerifyEmailSideEffect {
    data object Verified : VerifyEmailSideEffect
    data class ShowError(val message: String) : VerifyEmailSideEffect
    data class CodeResent(val message: String) : VerifyEmailSideEffect
}

