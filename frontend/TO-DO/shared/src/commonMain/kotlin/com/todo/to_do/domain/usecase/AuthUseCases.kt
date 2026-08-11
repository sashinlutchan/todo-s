package com.todo.to_do.domain.usecase

import com.todo.to_do.domain.model.AuthSession
import com.todo.to_do.domain.model.EmailVerificationOutcome
import com.todo.to_do.domain.model.PasswordResetOutcome
import com.todo.to_do.domain.model.ResetCodeVerification
import com.todo.to_do.domain.model.UserProfile
import com.todo.to_do.domain.repository.AuthRepository

class RegisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, phoneNumber: String, displayName: String? = null): String =
        repository.register(email, password, phoneNumber, displayName)
}

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AuthSession =
        repository.login(email, password)
}

class VerifyTokenUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Boolean = repository.verifyToken()
}

class GetProfileUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): UserProfile = repository.getProfile()
}

class ForgotPasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): String = repository.forgotPassword(email)
}

class VerifyResetCodeUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, code: String): ResetCodeVerification =
        repository.verifyResetCode(email, code)
}

class ResetPasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(resetToken: String, newPassword: String): PasswordResetOutcome =
        repository.resetPassword(resetToken, newPassword)
}

class LogoutUseCase(private val repository: AuthRepository) {
    operator fun invoke() = repository.logout()
}

class VerifyEmailUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, code: String): EmailVerificationOutcome =
        repository.verifyEmail(email, code)
}

class ResendVerificationCodeUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): String = repository.resendVerificationCode(email)
}
