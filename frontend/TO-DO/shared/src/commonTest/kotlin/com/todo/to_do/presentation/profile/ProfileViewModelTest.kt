package com.todo.to_do.presentation.profile

import com.todo.to_do.domain.model.UserProfile
import com.todo.to_do.domain.usecase.GetProfileUseCase
import com.todo.to_do.domain.usecase.LogoutUseCase
import com.todo.to_do.support.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.test
import kotlin.test.Test

class ProfileViewModelTest {

    private fun viewModel(repository: FakeAuthRepository) =
        ProfileViewModel(GetProfileUseCase(repository), LogoutUseCase(repository))

    @Test
    fun `load populates the profile fields on success`() = runTest {
        val repository = FakeAuthRepository(
            profile = UserProfile(
                id = "68a1f0c2e4b0a1a2b3c40001",
                email = "elena.martinez@protonmail.com",
                displayName = "Elena Martinez",
                createdAt = "2026-01-15T08:30:00Z"
            )
        )
        val vm = viewModel(repository)

        vm.test(this) {
            vm.load()
            expectState { copy(isLoading = true) }
            expectState {
                copy(
                    displayName = "Elena Martinez",
                    email = "elena.martinez@protonmail.com",
                    createdAt = "2026-01-15T08:30:00Z",
                    isLoading = false
                )
            }
        }
    }

    @Test
    fun `load surfaces a repository failure as ShowError`() = runTest {
        val repository = FakeAuthRepository(profile = null)
        val vm = viewModel(repository)

        vm.test(this) {
            vm.load()
            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false) }
            expectSideEffect(ProfileSideEffect.ShowError("no profile stubbed"))
        }
    }

    @Test
    fun `logout clears the session and posts LoggedOut`() = runTest {
        val repository = FakeAuthRepository()
        val vm = viewModel(repository)

        vm.test(this) {
            vm.logout()
            expectSideEffect(ProfileSideEffect.LoggedOut)
        }
        kotlin.test.assertTrue(repository.loggedOut)
    }
}
