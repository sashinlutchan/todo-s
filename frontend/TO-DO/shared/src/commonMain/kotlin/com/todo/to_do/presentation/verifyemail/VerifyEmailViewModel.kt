package com.todo.to_do.presentation.verifyemail

import androidx.lifecycle.ViewModel
import com.todo.to_do.domain.model.EmailVerificationOutcome
import com.todo.to_do.domain.usecase.ResendVerificationCodeUseCase
import com.todo.to_do.domain.usecase.VerifyEmailUseCase
import com.todo.to_do.util.toUserMessage
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class VerifyEmailViewModel(
    private val verifyEmail: VerifyEmailUseCase,
    private val resendVerificationCode: ResendVerificationCodeUseCase
) : ViewModel(), ContainerHost<VerifyEmailState, VerifyEmailSideEffect> {

    override val container = container<VerifyEmailState, VerifyEmailSideEffect>(VerifyEmailState())

    fun onCodeChange(code: String) = intent {
        reduce { state.copy(code = code.filter { it.isDigit() }.take(6)) }
    }

    fun verifyCode(email: String) = intent {
        if (state.code.length != 6) {
            postSideEffect(VerifyEmailSideEffect.ShowError("Enter the 6-digit code"))
            return@intent
        }

        reduce { state.copy(isLoading = true) }
        runCatching { verifyEmail(email, state.code) }
            .onSuccess { result ->
                reduce { state.copy(isLoading = false) }
                when (result) {
                    EmailVerificationOutcome.Verified ->
                        postSideEffect(VerifyEmailSideEffect.Verified)
                    is EmailVerificationOutcome.Failed ->
                        postSideEffect(VerifyEmailSideEffect.ShowError(reasonMessage(result.reason)))
                }
            }
            .onFailure {
                reduce { state.copy(isLoading = false) }
                postSideEffect(VerifyEmailSideEffect.ShowError(it.toUserMessage("Verification failed")))
            }
    }

    fun resendCode(email: String) = intent {
        runCatching { resendVerificationCode(email) }
            .onSuccess { postSideEffect(VerifyEmailSideEffect.CodeResent("A new code was sent to $email")) }
            .onFailure { postSideEffect(VerifyEmailSideEffect.ShowError(it.toUserMessage("Failed to resend code"))) }
    }
}

private fun reasonMessage(reason: String?): String = when (reason) {
    "CODE_EXPIRED" -> "That code has expired. Request a new one."
    "EMAIL_NOT_FOUND" -> "No pending verification found for that email."
    "CODE_INVALID" -> "That code isn't right. Check and try again."
    else -> "Verification failed"
}

