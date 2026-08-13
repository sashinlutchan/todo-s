package com.todo.to_do.presentation.profile

data class ProfileState(
    val displayName: String = "",
    val email: String = "",
    val createdAt: String = "",
    val isLoading: Boolean = false
)

sealed interface ProfileSideEffect {
    data class ShowError(val message: String) : ProfileSideEffect
    data object LoggedOut : ProfileSideEffect
}

