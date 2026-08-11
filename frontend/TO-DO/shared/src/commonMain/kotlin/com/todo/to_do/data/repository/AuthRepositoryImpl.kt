package com.todo.to_do.data.repository

import com.todo.to_do.data.remote.SessionStore
import com.todo.to_do.data.remote.TaskFlowApi
import com.todo.to_do.data.remote.dto.ForgotPasswordRequestDto
import com.todo.to_do.data.remote.dto.LoginRequestDto
import com.todo.to_do.data.remote.dto.RegisterRequestDto
import com.todo.to_do.data.remote.dto.ResendVerificationCodeRequestDto
import com.todo.to_do.data.remote.dto.ResetPasswordRequestDto
import com.todo.to_do.data.remote.dto.VerifyEmailRequestDto
import com.todo.to_do.data.remote.dto.VerifyResetCodeRequestDto
import com.todo.to_do.data.remote.dto.toDomain
import com.todo.to_do.domain.model.AuthSession
import com.todo.to_do.domain.model.EmailNotVerifiedException
import com.todo.to_do.domain.model.EmailVerificationOutcome
import com.todo.to_do.domain.model.PasswordResetOutcome
import com.todo.to_do.domain.model.ResetCodeVerification
import com.todo.to_do.domain.model.UserProfile
import com.todo.to_do.domain.repository.AuthRepository
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode

class AuthRepositoryImpl(
    private val api: TaskFlowApi,
    private val sessionStore: SessionStore
) : AuthRepository {

    override suspend fun register(email: String, password: String, phoneNumber: String, displayName: String?): String =
        api.register(RegisterRequestDto(email, password, displayName, phoneNumber)).message

    override suspend fun login(email: String, password: String): AuthSession {
        try {
            val session = api.login(LoginRequestDto(email, password)).toDomain()
            sessionStore.update(session)
            return session
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Forbidden) {
                throw EmailNotVerifiedException(email)
            }
            throw e
        }
    }

    override fun logout() = sessionStore.clear()

    override fun currentSession(): AuthSession? = sessionStore.session.value

    override suspend fun verifyToken(): Boolean {
        val response = api.verifyToken()
        if (!response.valid || response.user == null) {
            sessionStore.clear()
            return false
        }
        sessionStore.updateProfile(response.user.displayName, response.user.createdAt.toString())
        return true
    }

    override suspend fun getProfile(): UserProfile {
        val profile = api.getProfile().toDomain()
        // Persist fresh profile data to secure storage so the stored session stays up-to-date,
        // consistent with what verifyToken() does on every splash screen launch.
        sessionStore.updateProfile(profile.displayName, profile.createdAt)
        return profile
    }

    override suspend fun forgotPassword(email: String): String =
        api.forgotPassword(ForgotPasswordRequestDto(email)).message

    override suspend fun verifyResetCode(email: String, code: String): ResetCodeVerification {
        val response = api.verifyResetCode(VerifyResetCodeRequestDto(email, code))
        return if (response.valid && response.resetToken != null) {
            ResetCodeVerification.Valid(response.resetToken)
        } else {
            ResetCodeVerification.Invalid(response.reason)
        }
    }

    override suspend fun resetPassword(resetToken: String, newPassword: String): PasswordResetOutcome {
        val response = api.resetPassword(ResetPasswordRequestDto(resetToken, newPassword))
        return if (response.success) {
            PasswordResetOutcome.Success
        } else {
            PasswordResetOutcome.Failure(response.reason)
        }
    }

    override suspend fun verifyEmail(email: String, code: String): EmailVerificationOutcome {
        val response = api.verifyEmail(VerifyEmailRequestDto(email, code))
        if (response.success && response.token != null && response.userId != null && response.email != null) {
            sessionStore.update(
                AuthSession(
                    token = response.token,
                    userId = response.userId,
                    email = response.email,
                    displayName = response.displayName ?: ""
                )
            )
            return EmailVerificationOutcome.Verified
        }
        return EmailVerificationOutcome.Failed(response.reason)
    }

    override suspend fun resendVerificationCode(email: String): String =
        api.resendVerificationCode(ResendVerificationCodeRequestDto(email)).message
}
