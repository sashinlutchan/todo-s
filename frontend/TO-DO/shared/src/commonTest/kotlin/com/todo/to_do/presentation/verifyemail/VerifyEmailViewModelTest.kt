package com.todo.to_do.presentation.verifyemail

import com.todo.to_do.domain.model.EmailVerificationOutcome
import com.todo.to_do.domain.usecase.ResendVerificationCodeUseCase
import com.todo.to_do.domain.usecase.VerifyEmailUseCase
import com.todo.to_do.support.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.test
import kotlin.test.Test

class VerifyEmailViewModelTest {

    private fun viewModel(repository: FakeAuthRepository) =
        VerifyEmailViewModel(VerifyEmailUseCase(repository), ResendVerificationCodeUseCase(repository))

    @Test
    fun `verifyCode with a correct code posts Verified`() = runTest {
        val repository = FakeAuthRepository(
            verifyEmailResult = Result.success(EmailVerificationOutcome.Verified)
        )
        val vm = viewModel(repository)

        vm.test(this) {
            vm.onCodeChange("483920")
            expectState { copy(code = "483920") }

            vm.verifyCode("elena.martinez@protonmail.com")
            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false) }
            expectSideEffect(VerifyEmailSideEffect.Verified)
        }
    }

    @Test
    fun `verifyCode with an expired code shows a friendly expiry message`() = runTest {
        val repository = FakeAuthRepository(
            verifyEmailResult = Result.success(EmailVerificationOutcome.Failed("CODE_EXPIRED"))
        )
        val vm = viewModel(repository)

        vm.test(this) {
            vm.onCodeChange("483920")
            expectState { copy(code = "483920") }

            vm.verifyCode("elena.martinez@protonmail.com")
            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false) }
            expectSideEffect(VerifyEmailSideEffect.ShowError("That code has expired. Request a new one."))
        }
    }

    @Test
    fun `verifyCode rejects an incomplete code before calling the repository`() = runTest {
        val repository = FakeAuthRepository()
        val vm = viewModel(repository)

        vm.test(this) {
            vm.onCodeChange("123")
            expectState { copy(code = "123") }

            vm.verifyCode("elena.martinez@protonmail.com")
            expectSideEffect(VerifyEmailSideEffect.ShowError("Enter the 6-digit code"))
        }
    }

    @Test
    fun `resendCode posts CodeResent on success`() = runTest {
        val repository = FakeAuthRepository(resendVerificationCodeResult = Result.success("sent"))
        val vm = viewModel(repository)

        vm.test(this) {
            vm.resendCode("elena.martinez@protonmail.com")
            expectSideEffect(VerifyEmailSideEffect.CodeResent("A new code was sent to elena.martinez@protonmail.com"))
        }
    }

    @Test
    fun `onCodeChange strips non-digits and caps at 6 characters`() = runTest {
        val repository = FakeAuthRepository()
        val vm = viewModel(repository)

        vm.test(this) {
            vm.onCodeChange("4a8-3920999")
            expectState { copy(code = "483920") }
        }
    }
}
