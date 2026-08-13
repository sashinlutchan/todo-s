package com.todo.to_do.support

import com.todo.to_do.domain.model.AuthSession
import com.todo.to_do.domain.model.EmailVerificationOutcome
import com.todo.to_do.domain.model.PasswordResetOutcome
import com.todo.to_do.domain.model.ResetCodeVerification
import com.todo.to_do.domain.model.UserProfile
import com.todo.to_do.domain.repository.AuthRepository

class FakeAuthRepository(
    private var session: AuthSession? = null,
    var profile: UserProfile? = null,
    var verifyTokenResult: Result<Boolean> = Result.success(true),
    var forgotPasswordResult: Result<String> = Result.success("If that email exists, a reset code has been sent"),
    var verifyResetCodeResult: Result<ResetCodeVerification> = Result.success(ResetCodeVerification.Invalid("CODE_INVALID")),
    var resetPasswordResult: Result<PasswordResetOutcome> = Result.success(PasswordResetOutcome.Success),
    var registerResult: Result<String> = Result.success("Account created. Enter the code we sent to verify your email."),
    var verifyEmailResult: Result<EmailVerificationOutcome> = Result.success(EmailVerificationOutcome.Failed("CODE_INVALID")),
    var resendVerificationCodeResult: Result<String> =
        Result.success("If that account needs verification, a new code has been sent"),
    var loginResult: Result<AuthSession>? = null
) : AuthRepository {

    var loggedOut = false
        private set

    override suspend fun register(email: String, password: String, phoneNumber: String, displayName: String?): String =
        registerResult.getOrThrow()

    override suspend fun login(email: String, password: String): AuthSession =
        (loginResult ?: throw UnsupportedOperationException("not needed by these tests")).getOrThrow()

    override fun logout() {
        loggedOut = true
        session = null
    }

    override fun currentSession(): AuthSession? = session

    override suspend fun verifyToken(): Boolean = verifyTokenResult.getOrThrow()

    override suspend fun getProfile(): UserProfile = profile ?: throw IllegalStateException("no profile stubbed")

    override suspend fun forgotPassword(email: String): String = forgotPasswordResult.getOrThrow()

    override suspend fun verifyResetCode(email: String, code: String): ResetCodeVerification =
        verifyResetCodeResult.getOrThrow()

    override suspend fun resetPassword(resetToken: String, newPassword: String): PasswordResetOutcome =
        resetPasswordResult.getOrThrow()

    override suspend fun verifyEmail(email: String, code: String): EmailVerificationOutcome =
        verifyEmailResult.getOrThrow()

    override suspend fun resendVerificationCode(email: String): String =
        resendVerificationCodeResult.getOrThrow()
}

