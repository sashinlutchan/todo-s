package com.todo.to_do.presentation.resetpassword

import com.todo.to_do.domain.model.PasswordResetOutcome
import com.todo.to_do.domain.usecase.ResetPasswordUseCase
import com.todo.to_do.support.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.test
import kotlin.test.Test

class ResetPasswordViewModelTest {

    private fun viewModel(repository: FakeAuthRepository) =
        ResetPasswordViewModel(ResetPasswordUseCase(repository))

    @Test
    fun `submit with matching strong passwords posts Success`() = runTest {
        val repository = FakeAuthRepository(resetPasswordResult = Result.success(PasswordResetOutcome.Success))
        val vm = viewModel(repository)

        vm.test(this) {
            vm.onPasswordChange("NewSecureP@ss123")
            expectState { copy(newPassword = "NewSecureP@ss123") }
            vm.onConfirmPasswordChange("NewSecureP@ss123")
            expectState { copy(confirmPassword = "NewSecureP@ss123") }

            vm.submit("short.lived.reset.token")
            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false) }
            expectSideEffect(ResetPasswordSideEffect.Success)
        }
    }

    @Test
    fun `submit rejects a too-short password before calling the repository`() = runTest {
        val repository = FakeAuthRepository()
        val vm = viewModel(repository)

        vm.test(this) {
            vm.onPasswordChange("short")
            expectState { copy(newPassword = "short") }
            vm.onConfirmPasswordChange("short")
            expectState { copy(confirmPassword = "short") }

            vm.submit("short.lived.reset.token")
            expectSideEffect(ResetPasswordSideEffect.ShowError("Password must be at least 8 characters"))
        }
    }

    @Test
    fun `submit rejects mismatched passwords before calling the repository`() = runTest {
        val repository = FakeAuthRepository()
        val vm = viewModel(repository)

        vm.test(this) {
            vm.onPasswordChange("NewSecureP@ss123")
            expectState { copy(newPassword = "NewSecureP@ss123") }
            vm.onConfirmPasswordChange("DifferentPass1")
            expectState { copy(confirmPassword = "DifferentPass1") }

            vm.submit("short.lived.reset.token")
            expectSideEffect(ResetPasswordSideEffect.ShowError("Passwords don't match"))
        }
    }

    @Test
    fun `submit maps an expired reset token to a friendly message`() = runTest {
        val repository = FakeAuthRepository(
            resetPasswordResult = Result.success(PasswordResetOutcome.Failure("RESET_TOKEN_EXPIRED"))
        )
        val vm = viewModel(repository)

        vm.test(this) {
            vm.onPasswordChange("NewSecureP@ss123")
            expectState { copy(newPassword = "NewSecureP@ss123") }
            vm.onConfirmPasswordChange("NewSecureP@ss123")
            expectState { copy(confirmPassword = "NewSecureP@ss123") }

            vm.submit("stale.token")
            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false) }
            expectSideEffect(ResetPasswordSideEffect.ShowError("This reset link has expired. Start over."))
        }
    }
}

