package com.todo.to_do.presentation.resetpassword

import androidx.lifecycle.ViewModel
import com.todo.to_do.domain.model.PasswordResetOutcome
import com.todo.to_do.domain.usecase.ResetPasswordUseCase
import com.todo.to_do.util.toUserMessage
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

private const val MIN_PASSWORD_LENGTH = 8

class ResetPasswordViewModel(
    private val resetPassword: ResetPasswordUseCase
) : ViewModel(), ContainerHost<ResetPasswordState, ResetPasswordSideEffect> {

    override val container = container<ResetPasswordState, ResetPasswordSideEffect>(ResetPasswordState())

    fun onPasswordChange(value: String) = intent { reduce { state.copy(newPassword = value) } }

    fun onConfirmPasswordChange(value: String) = intent { reduce { state.copy(confirmPassword = value) } }

    fun submit(resetToken: String) = intent {
        if (state.newPassword.length < MIN_PASSWORD_LENGTH) {
            postSideEffect(ResetPasswordSideEffect.ShowError("Password must be at least 8 characters"))
            return@intent
        }
        if (state.newPassword != state.confirmPassword) {
            postSideEffect(ResetPasswordSideEffect.ShowError("Passwords don't match"))
            return@intent
        }

        reduce { state.copy(isLoading = true) }
        runCatching { resetPassword(resetToken, state.newPassword) }
            .onSuccess { outcome ->
                reduce { state.copy(isLoading = false) }
                when (outcome) {
                    PasswordResetOutcome.Success -> postSideEffect(ResetPasswordSideEffect.Success)
                    is PasswordResetOutcome.Failure ->
                        postSideEffect(ResetPasswordSideEffect.ShowError(reasonMessage(outcome.reason)))
                }
            }
            .onFailure {
                reduce { state.copy(isLoading = false) }
                postSideEffect(ResetPasswordSideEffect.ShowError(it.toUserMessage("Reset failed")))
            }
    }
}

private fun reasonMessage(reason: String?): String = when (reason) {
    "RESET_TOKEN_EXPIRED" -> "This reset link has expired. Start over."
    "RESET_TOKEN_INVALID" -> "This reset link is invalid. Start over."
    "PASSWORD_TOO_SHORT" -> "Password must be at least 8 characters."
    "USER_NOT_FOUND" -> "Account not found."
    else -> "Reset failed"
}
