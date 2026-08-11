package com.todo.to_do.domain.repository

import com.todo.to_do.domain.model.AuthSession
import com.todo.to_do.domain.model.EmailVerificationOutcome
import com.todo.to_do.domain.model.PasswordResetOutcome
import com.todo.to_do.domain.model.ResetCodeVerification
import com.todo.to_do.domain.model.UserProfile

interface AuthRepository {
    /** Creates an unverified account and texts an OTP; returns a status message (no session yet). */
    suspend fun register(email: String, password: String, phoneNumber: String, displayName: String? = null): String
    suspend fun login(email: String, password: String): AuthSession
    fun logout()
    fun currentSession(): AuthSession?

    /** Confirms the stored session's token is still valid, hydrating profile fields on success. */
    suspend fun verifyToken(): Boolean
    suspend fun getProfile(): UserProfile
    suspend fun forgotPassword(email: String): String
    suspend fun verifyResetCode(email: String, code: String): ResetCodeVerification
    suspend fun resetPassword(resetToken: String, newPassword: String): PasswordResetOutcome

    /** Verifies the signup OTP; on success persists the returned session (auto-login). */
    suspend fun verifyEmail(email: String, code: String): EmailVerificationOutcome
    suspend fun resendVerificationCode(email: String): String
}
