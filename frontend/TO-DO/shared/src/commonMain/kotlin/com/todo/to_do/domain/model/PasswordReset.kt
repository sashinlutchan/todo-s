package com.todo.to_do.domain.model

sealed interface ResetCodeVerification {
    data class Valid(val resetToken: String) : ResetCodeVerification
    data class Invalid(val reason: String?) : ResetCodeVerification
}

sealed interface PasswordResetOutcome {
    data object Success : PasswordResetOutcome
    data class Failure(val reason: String?) : PasswordResetOutcome
}
