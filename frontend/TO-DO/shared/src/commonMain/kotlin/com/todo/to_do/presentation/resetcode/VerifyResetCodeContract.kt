package com.todo.to_do.presentation.resetcode

data class VerifyResetCodeState(
    val code: String = "",
    val isLoading: Boolean = false
)

sealed interface VerifyResetCodeSideEffect {
    data class Verified(val resetToken: String) : VerifyResetCodeSideEffect
    data class ShowError(val message: String) : VerifyResetCodeSideEffect
    data class CodeResent(val message: String) : VerifyResetCodeSideEffect
}

