package com.todo.to_do.presentation.auth

import androidx.lifecycle.ViewModel
import com.todo.to_do.domain.model.EmailNotVerifiedException
import com.todo.to_do.domain.usecase.LoginUseCase
import com.todo.to_do.domain.usecase.RegisterUseCase
import com.todo.to_do.util.toUserMessage
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class AuthViewModel(
    private val login: LoginUseCase,
    private val register: RegisterUseCase
) : ViewModel(), ContainerHost<AuthState, AuthSideEffect> {

    override val container = container<AuthState, AuthSideEffect>(AuthState())

    fun onEmailChange(email: String) = intent { reduce { state.copy(email = email) } }

    fun onPasswordChange(password: String) = intent { reduce { state.copy(password = password) } }

    fun onDisplayNameChange(displayName: String) = intent { reduce { state.copy(displayName = displayName) } }

    fun onPhoneNumberChange(phoneNumber: String) = intent { reduce { state.copy(phoneNumber = phoneNumber) } }

    fun toggleMode() = intent { reduce { state.copy(isRegisterMode = !state.isRegisterMode) } }

    fun submit() = intent {
        reduce { state.copy(isLoading = true) }
        if (state.isRegisterMode) {
            runCatching {
                register(state.email, state.password, state.phoneNumber.trim(), state.displayName.trim().ifBlank { null })
            }
                .onSuccess {
                    reduce { state.copy(isLoading = false) }
                    postSideEffect(AuthSideEffect.NeedsVerification(state.email))
                }
                .onFailure {
                    reduce { state.copy(isLoading = false) }
                    postSideEffect(AuthSideEffect.ShowError(it.toUserMessage("Registration failed")))
                }
        } else {
            runCatching { login(state.email, state.password) }
                .onSuccess {
                    reduce { state.copy(isLoading = false) }
                    postSideEffect(AuthSideEffect.Authenticated)
                }
                .onFailure { error ->
                    reduce { state.copy(isLoading = false) }
                    if (error is EmailNotVerifiedException) {
                        postSideEffect(AuthSideEffect.NeedsVerification(error.email))
                    } else {
                        postSideEffect(AuthSideEffect.ShowError(error.toUserMessage("Authentication failed")))
                    }
                }
        }
    }
}
