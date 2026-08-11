package com.todo.to_do.presentation.resetpassword

data class ResetPasswordState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false
)

sealed interface ResetPasswordSideEffect {
    data object Success : ResetPasswordSideEffect
    data class ShowError(val message: String) : ResetPasswordSideEffect
}
