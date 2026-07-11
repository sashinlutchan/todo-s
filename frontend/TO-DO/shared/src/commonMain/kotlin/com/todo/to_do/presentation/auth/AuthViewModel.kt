package com.todo.to_do.presentation.auth

import androidx.lifecycle.ViewModel
import com.todo.to_do.domain.usecase.LoginUseCase
import com.todo.to_do.domain.usecase.RegisterUseCase
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class AuthViewModel(
    private val login: LoginUseCase,
    private val register: RegisterUseCase
) : ViewModel(), ContainerHost<AuthState, AuthSideEffect> {

    override val container = container<AuthState, AuthSideEffect>(AuthState())

    fun onEmailChange(email: String) = intent { reduce { state.copy(email = email) } }

    fun onPasswordChange(password: String) = intent { reduce { state.copy(password = password) } }

    fun toggleMode() = intent { reduce { state.copy(isRegisterMode = !state.isRegisterMode) } }

    fun submit() = intent {
        reduce { state.copy(isLoading = true) }
        val result = runCatching {
            if (state.isRegisterMode) register(state.email, state.password)
            else login(state.email, state.password)
        }
        reduce { state.copy(isLoading = false) }
        result
            .onSuccess { postSideEffect(AuthSideEffect.Authenticated) }
            .onFailure { postSideEffect(AuthSideEffect.ShowError(it.message ?: "Authentication failed")) }
    }
}
