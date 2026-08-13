package com.todo.to_do.presentation.resetcode

import androidx.lifecycle.ViewModel
import com.todo.to_do.domain.model.ResetCodeVerification
import com.todo.to_do.domain.usecase.ForgotPasswordUseCase
import com.todo.to_do.domain.usecase.VerifyResetCodeUseCase
import com.todo.to_do.util.toUserMessage
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class VerifyResetCodeViewModel(
    private val verifyResetCode: VerifyResetCodeUseCase,
    private val forgotPassword: ForgotPasswordUseCase
) : ViewModel(), ContainerHost<VerifyResetCodeState, VerifyResetCodeSideEffect> {

    override val container = container<VerifyResetCodeState, VerifyResetCodeSideEffect>(VerifyResetCodeState())

    fun onCodeChange(code: String) = intent {
        reduce { state.copy(code = code.filter { it.isDigit() }.take(6)) }
    }

    fun verifyCode(email: String) = intent {
        if (state.code.length != 6) {
            postSideEffect(VerifyResetCodeSideEffect.ShowError("Enter the 6-digit code"))
            return@intent
        }

        reduce { state.copy(isLoading = true) }
        runCatching { verifyResetCode(email, state.code) }
            .onSuccess { result ->
                reduce { state.copy(isLoading = false) }
                when (result) {
                    is ResetCodeVerification.Valid ->
                        postSideEffect(VerifyResetCodeSideEffect.Verified(result.resetToken))
                    is ResetCodeVerification.Invalid ->
                        postSideEffect(VerifyResetCodeSideEffect.ShowError(reasonMessage(result.reason)))
                }
            }
            .onFailure {
                reduce { state.copy(isLoading = false) }
                postSideEffect(VerifyResetCodeSideEffect.ShowError(it.toUserMessage("Verification failed")))
            }
    }

    fun resendCode(email: String) = intent {
        runCatching { forgotPassword(email) }
            .onSuccess { postSideEffect(VerifyResetCodeSideEffect.CodeResent("A new code was sent to $email")) }
            .onFailure { postSideEffect(VerifyResetCodeSideEffect.ShowError(it.toUserMessage("Failed to resend code"))) }
    }
}

private fun reasonMessage(reason: String?): String = when (reason) {
    "CODE_EXPIRED" -> "That code has expired. Request a new one."
    "CODE_ALREADY_USED" -> "That code was already used. Request a new one."
    "EMAIL_NOT_FOUND" -> "No reset request found for that email."
    "CODE_INVALID" -> "That code isn't right. Check and try again."
    else -> "Verification failed"
}

