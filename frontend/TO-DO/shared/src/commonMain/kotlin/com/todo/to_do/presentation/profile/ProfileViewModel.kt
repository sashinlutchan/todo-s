package com.todo.to_do.presentation.profile

import androidx.lifecycle.ViewModel
import com.todo.to_do.domain.usecase.GetProfileUseCase
import com.todo.to_do.domain.usecase.LogoutUseCase
import com.todo.to_do.util.toUserMessage
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class ProfileViewModel(
    private val getProfile: GetProfileUseCase,
    private val logout: LogoutUseCase
) : ViewModel(), ContainerHost<ProfileState, ProfileSideEffect> {

    override val container = container<ProfileState, ProfileSideEffect>(ProfileState())

    fun load() = intent {
        reduce { state.copy(isLoading = true) }
        runCatching { getProfile() }
            .onSuccess { profile ->
                reduce {
                    state.copy(
                        displayName = profile.displayName,
                        email = profile.email,
                        createdAt = profile.createdAt,
                        isLoading = false
                    )
                }
            }
            .onFailure {
                reduce { state.copy(isLoading = false) }
                postSideEffect(ProfileSideEffect.ShowError(it.toUserMessage("Failed to load profile")))
            }
    }

    fun logout() = intent {
        logout.invoke()
        postSideEffect(ProfileSideEffect.LoggedOut)
    }
}
