package com.todo.to_do.domain.model

sealed interface EmailVerificationOutcome {
    data object Verified : EmailVerificationOutcome
    data class Failed(val reason: String?) : EmailVerificationOutcome
}
