package com.todo.to_do.presentation.forgotpassword

import com.todo.to_do.domain.usecase.ForgotPasswordUseCase
import com.todo.to_do.support.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.test
import kotlin.test.Test

class ForgotPasswordViewModelTest {

    @Test
    fun `submit with a valid email sends the code and reports CodeSent`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = ForgotPasswordViewModel(ForgotPasswordUseCase(repository))

        viewModel.test(this) {
            viewModel.onEmailChange("elena.martinez@protonmail.com")
            expectState { copy(email = "elena.martinez@protonmail.com") }

            viewModel.submit()
            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false) }
            expectSideEffect(ForgotPasswordSideEffect.CodeSent("elena.martinez@protonmail.com"))
        }
    }

    @Test
    fun `submit with an invalid email shows an error and never calls the repository`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = ForgotPasswordViewModel(ForgotPasswordUseCase(repository))

        viewModel.test(this) {
            viewModel.onEmailChange("not-an-email")
            expectState { copy(email = "not-an-email") }

            viewModel.submit()
            expectSideEffect(ForgotPasswordSideEffect.ShowError("Enter a valid email"))
        }
    }

    @Test
    fun `submit surfaces a repository failure as ShowError`() = runTest {
        val repository = FakeAuthRepository(forgotPasswordResult = Result.failure(RuntimeException("offline")))
        val viewModel = ForgotPasswordViewModel(ForgotPasswordUseCase(repository))

        viewModel.test(this) {
            viewModel.onEmailChange("elena.martinez@protonmail.com")
            expectState { copy(email = "elena.martinez@protonmail.com") }

            viewModel.submit()
            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false) }
            expectSideEffect(ForgotPasswordSideEffect.ShowError("offline"))
        }
    }
}
