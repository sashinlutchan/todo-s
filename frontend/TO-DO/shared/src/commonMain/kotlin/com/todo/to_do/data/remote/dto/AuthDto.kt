package com.todo.to_do.data.remote.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    val displayName: String? = null,
    val phoneNumber: String? = null
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponseDto(
    val token: String,
    val userId: String,
    val email: String,
    val displayName: String = ""
)

@Serializable
data class RegisterResponseDto(
    val message: String,
    val email: String
)

@Serializable
data class UserProfileDto(
    val id: String,
    val email: String,
    val displayName: String,
    val createdAt: Instant
)

@Serializable
data class VerifyTokenResponseDto(
    val valid: Boolean,
    val user: UserProfileDto? = null,
    val tokenExpiresAt: Instant? = null,
    val reason: String? = null
)

@Serializable
data class ForgotPasswordRequestDto(
    val email: String
)

@Serializable
data class ForgotPasswordResponseDto(
    val message: String
)

@Serializable
data class VerifyResetCodeRequestDto(
    val email: String,
    val code: String
)

@Serializable
data class VerifyResetCodeResponseDto(
    val valid: Boolean,
    val resetToken: String? = null,
    val expiresAt: Instant? = null,
    val reason: String? = null
)

@Serializable
data class ResetPasswordRequestDto(
    val resetToken: String,
    val newPassword: String
)

@Serializable
data class ResetPasswordResponseDto(
    val success: Boolean,
    val message: String? = null,
    val reason: String? = null
)

@Serializable
data class VerifyEmailRequestDto(
    val email: String,
    val code: String
)

@Serializable
data class VerifyEmailResponseDto(
    val success: Boolean,
    val token: String? = null,
    val userId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val reason: String? = null
)

@Serializable
data class ResendVerificationCodeRequestDto(
    val email: String
)

@Serializable
data class ResendVerificationCodeResponseDto(
    val message: String
)
