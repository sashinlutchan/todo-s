package com.todo.to_do.presentation.auth

import com.todo.to_do.domain.model.AuthSession
import com.todo.to_do.domain.model.EmailNotVerifiedException
import com.todo.to_do.domain.usecase.LoginUseCase
import com.todo.to_do.domain.usecase.RegisterUseCase
import com.todo.to_do.support.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.test
import kotlin.test.Test

class AuthViewModelTest {

    private fun viewModel(repository: FakeAuthRepository) =
        AuthViewModel(LoginUseCase(repository), RegisterUseCase(repository))

    @Test
    fun `submit in login mode posts Authenticated on success`() = runTest {
        val repository = FakeAuthRepository(
            loginResult = Result.success(
                AuthSession(token = "signed.jwt", userId = "68a1f0c2e4b0a1a2b3c40001", email = "elena.martinez@protonmail.com")
            )
        )
        val vm = viewModel(repository)

        vm.test(this) {
            vm.onEmailChange("elena.martinez@protonmail.com")
            expectState { copy(email = "elena.martinez@protonmail.com") }
            vm.onPasswordChange("Tr0ubad0ur&3xpanse!")
            expectState { copy(password = "Tr0ubad0ur&3xpanse!") }

            vm.submit()
            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false) }
            expectSideEffect(AuthSideEffect.Authenticated)
        }
    }

    @Test
    fun `submit in login mode against an unverified account posts NeedsVerification`() = runTest {
        val repository = FakeAuthRepository(
            loginResult = Result.failure(EmailNotVerifiedException("elena.martinez@protonmail.com"))
        )
        val vm = viewModel(repository)

        vm.test(this) {
            vm.onEmailChange("elena.martinez@protonmail.com")
            expectState { copy(email = "elena.martinez@protonmail.com") }
            vm.onPasswordChange("Tr0ubad0ur&3xpanse!")
            expectState { copy(password = "Tr0ubad0ur&3xpanse!") }

            vm.submit()
            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false) }
            expectSideEffect(AuthSideEffect.NeedsVerification("elena.martinez@protonmail.com"))
        }
    }

    @Test
    fun `submit in register mode posts NeedsVerification instead of Authenticated`() = runTest {
        val repository = FakeAuthRepository(
            registerResult = Result.success("Account created. Enter the code we sent to verify your email.")
        )
        val vm = viewModel(repository)

        vm.test(this) {
            vm.toggleMode()
            expectState { copy(isRegisterMode = true) }
            vm.onEmailChange("marcus.chen@outlook.com")
            expectState { copy(email = "marcus.chen@outlook.com") }
            vm.onPasswordChange("correct-horse-battery-staple")
            expectState { copy(password = "correct-horse-battery-staple") }
            vm.onPhoneNumberChange("+15551230002")
            expectState { copy(phoneNumber = "+15551230002") }

            vm.submit()
            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false) }
            expectSideEffect(AuthSideEffect.NeedsVerification("marcus.chen@outlook.com"))
        }
    }
}
