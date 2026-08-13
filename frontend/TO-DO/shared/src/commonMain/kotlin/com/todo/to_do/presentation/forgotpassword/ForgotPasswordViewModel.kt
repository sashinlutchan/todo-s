package com.todo.to_do.presentation.forgotpassword

import androidx.lifecycle.ViewModel
import com.todo.to_do.domain.usecase.ForgotPasswordUseCase
import com.todo.to_do.util.toUserMessage
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class ForgotPasswordViewModel(
    private val forgotPassword: ForgotPasswordUseCase
) : ViewModel(), ContainerHost<ForgotPasswordState, ForgotPasswordSideEffect> {

    override val container = container<ForgotPasswordState, ForgotPasswordSideEffect>(ForgotPasswordState())

    fun onEmailChange(email: String) = intent { reduce { state.copy(email = email) } }

    fun submit() = intent {
        val email = state.email.trim()
        if (!email.contains("@") || email.length < 5) {
            postSideEffect(ForgotPasswordSideEffect.ShowError("Enter a valid email"))
            return@intent
        }

        reduce { state.copy(isLoading = true) }
        runCatching { forgotPassword(email) }
            .onSuccess {
                reduce { state.copy(isLoading = false) }
                postSideEffect(ForgotPasswordSideEffect.CodeSent(email))
            }
            .onFailure {
                reduce { state.copy(isLoading = false) }
                postSideEffect(ForgotPasswordSideEffect.ShowError(it.toUserMessage("Failed to send reset code")))
            }
    }
}

