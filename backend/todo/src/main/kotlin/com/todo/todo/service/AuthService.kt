package com.todo.todo.service

import com.todo.todo.dto.AuthRequest
import com.todo.todo.dto.AuthResponse
import com.todo.todo.dto.ForgotPasswordRequest
import com.todo.todo.dto.ForgotPasswordResponse
import com.todo.todo.dto.RegisterResponse
import com.todo.todo.dto.ResendVerificationCodeRequest
import com.todo.todo.dto.ResendVerificationCodeResponse
import com.todo.todo.dto.ResetPasswordRequest
import com.todo.todo.dto.ResetPasswordResponse
import com.todo.todo.dto.UserProfileDto
import com.todo.todo.dto.VerifyEmailRequest
import com.todo.todo.dto.VerifyEmailResponse
import com.todo.todo.dto.VerifyResetCodeRequest
import com.todo.todo.dto.VerifyResetCodeResponse
import com.todo.todo.dto.VerifyTokenResponse
import com.todo.todo.entity.EmailVerificationEntity
import com.todo.todo.entity.PasswordResetEntity
import com.todo.todo.entity.UserEntity
import com.todo.todo.exception.EmailAlreadyInUseException
import com.todo.todo.exception.EmailNotVerifiedException
import com.todo.todo.exception.InvalidCredentialsException
import com.todo.todo.exception.InvalidPhoneNumberException
import com.todo.todo.exception.MissingPhoneNumberException
import com.todo.todo.exception.UserNotFoundException
import com.todo.todo.repository.EmailVerificationRepository
import com.todo.todo.repository.PasswordResetRepository
import com.todo.todo.repository.UserRepository
import com.todo.todo.repository.security.JwtService
import com.todo.todo.repository.security.ResetTokenValidation
import com.todo.todo.repository.security.TokenValidation
import com.todo.todo.service.sms.SmsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val RESET_CODE_TTL_MINUTES = 15L
private const val VERIFICATION_CODE_TTL_MINUTES = 15L
private const val MIN_PASSWORD_LENGTH = 8

// E.164: a leading '+', a non-zero country code digit, then up to 14 more digits.
private val E164_PHONE_REGEX = Regex("^\\+[1-9]\\d{7,14}$")

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordResetRepository: PasswordResetRepository,
    private val emailVerificationRepository: EmailVerificationRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val smsService: SmsService
) {

    suspend fun register(request: AuthRequest): RegisterResponse {
        val existingUser = userRepository.findByEmail(request.email)
        if (existingUser != null) {
            throw EmailAlreadyInUseException(request.email)
        }

        val phoneNumber = request.phoneNumber?.trim()
        if (phoneNumber.isNullOrBlank()) {
            throw MissingPhoneNumberException()
        }
        if (!E164_PHONE_REGEX.matches(phoneNumber)) {
            throw InvalidPhoneNumberException()
        }

        val hashedPassword = passwordEncoder.encode(request.password)!!
        val now = Instant.now()
        val userEntity = UserEntity(
            email = request.email,
            passwordHash = hashedPassword,
            phoneNumber = phoneNumber,
            displayName = request.displayName?.trim().orDefaultDisplayName(request.email),
            emailVerified = false,
            createdAt = now,
            updatedAt = now
        )

        val savedUser = userRepository.save(userEntity)
        sendVerificationCode(savedUser.email, savedUser.phoneNumber, savedUser.displayName)

        return RegisterResponse(
            message = "Account created. Enter the code we texted to your phone to verify your account.",
            email = savedUser.email
        )
    }

    suspend fun login(request: AuthRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        if (!user.emailVerified) {
            throw EmailNotVerifiedException(user.email)
        }

        val userId = user.id!!
        val token = jwtService.generateToken(userId, user.email)

        return AuthResponse(
            token = token,
            userId = userId,
            email = user.email,
            displayName = user.displayName
        )
    }

    /**
     * Manually validates the bearer token rather than relying on the security filter chain,
     * since an expired/invalid token here is an expected outcome to report back to the caller,
     * not a request to reject at the gate.
     */
    suspend fun verifyToken(token: String): VerifyTokenResponse {
        return when (val validation = jwtService.validateToken(token)) {
            is TokenValidation.Valid -> {
                val user = userRepository.findById(validation.principal.userId)
                    ?: return VerifyTokenResponse(valid = false, reason = "USER_NOT_FOUND")

                VerifyTokenResponse(
                    valid = true,
                    user = user.toProfileDto(),
                    tokenExpiresAt = validation.expiresAt
                )
            }
            TokenValidation.Expired -> VerifyTokenResponse(valid = false, reason = "TOKEN_EXPIRED")
            TokenValidation.Invalid -> VerifyTokenResponse(valid = false, reason = "TOKEN_INVALID")
        }
    }

    suspend fun getProfile(userId: String): UserProfileDto {
        val user = userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        return user.toProfileDto()
    }

    suspend fun sendForgotPasswordCode(request: ForgotPasswordRequest): ForgotPasswordResponse {
        val user = userRepository.findByEmail(request.email)

        if (user != null) {
            val code = (100000..999999).random().toString()
            val hashedCode = passwordEncoder.encode(code)!!

            passwordResetRepository.deleteByEmail(request.email)
            passwordResetRepository.save(
                PasswordResetEntity(
                    email = request.email,
                    codeHash = hashedCode,
                    expiresAt = Instant.now().plus(RESET_CODE_TTL_MINUTES, ChronoUnit.MINUTES)
                )
            )

            smsService.sendResetCode(
                to = user.phoneNumber,
                displayName = user.displayName.ifBlank { request.email },
                code = code
            )
        }

        // Always return the same response regardless of whether the email exists,
        // so this endpoint can't be used to enumerate registered accounts.
        return ForgotPasswordResponse(message = "If that email exists, a reset code has been texted to the phone on file")
    }

    suspend fun verifyResetCode(request: VerifyResetCodeRequest): VerifyResetCodeResponse {
        val record = passwordResetRepository.findFirstByEmailOrderByCreatedAtDesc(request.email)
            ?: return VerifyResetCodeResponse(valid = false, reason = "EMAIL_NOT_FOUND")

        if (record.used) {
            return VerifyResetCodeResponse(valid = false, reason = "CODE_ALREADY_USED")
        }

        if (Instant.now().isAfter(record.expiresAt)) {
            return VerifyResetCodeResponse(valid = false, reason = "CODE_EXPIRED")
        }

        if (!passwordEncoder.matches(request.code, record.codeHash)) {
            return VerifyResetCodeResponse(valid = false, reason = "CODE_INVALID")
        }

        passwordResetRepository.save(record.copy(verified = true))

        val resetToken = jwtService.generateResetToken(request.email)

        return VerifyResetCodeResponse(
            valid = true,
            resetToken = resetToken,
            expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES)
        )
    }

    suspend fun resetPassword(request: ResetPasswordRequest): ResetPasswordResponse {
        val email = when (val validation = jwtService.validateResetToken(request.resetToken)) {
            is ResetTokenValidation.Valid -> validation.email
            ResetTokenValidation.Expired -> return ResetPasswordResponse(success = false, reason = "RESET_TOKEN_EXPIRED")
            ResetTokenValidation.Invalid -> return ResetPasswordResponse(success = false, reason = "RESET_TOKEN_INVALID")
        }

        if (request.newPassword.length < MIN_PASSWORD_LENGTH) {
            return ResetPasswordResponse(success = false, reason = "PASSWORD_TOO_SHORT")
        }

        val user = userRepository.findByEmail(email)
            ?: return ResetPasswordResponse(success = false, reason = "USER_NOT_FOUND")

        val record = passwordResetRepository.findFirstByEmailOrderByCreatedAtDesc(email)
        if (record == null || !record.verified || record.used) {
            return ResetPasswordResponse(success = false, reason = "RESET_TOKEN_INVALID")
        }

        userRepository.save(
            user.copy(
                passwordHash = passwordEncoder.encode(request.newPassword)!!,
                updatedAt = Instant.now()
            )
        )
        passwordResetRepository.deleteByEmail(email)

        return ResetPasswordResponse(success = true, message = "Password reset successfully. Please log in.")
    }

    suspend fun verifyEmail(request: VerifyEmailRequest): VerifyEmailResponse {
        val record = emailVerificationRepository.findFirstByEmailOrderByCreatedAtDesc(request.email)
            ?: return VerifyEmailResponse(success = false, reason = "EMAIL_NOT_FOUND")

        if (Instant.now().isAfter(record.expiresAt)) {
            return VerifyEmailResponse(success = false, reason = "CODE_EXPIRED")
        }

        if (!passwordEncoder.matches(request.code, record.codeHash)) {
            return VerifyEmailResponse(success = false, reason = "CODE_INVALID")
        }

        val user = userRepository.findByEmail(request.email)
            ?: return VerifyEmailResponse(success = false, reason = "USER_NOT_FOUND")

        val updatedUser = userRepository.save(user.copy(emailVerified = true, updatedAt = Instant.now()))
        emailVerificationRepository.deleteByEmail(request.email)

        val userId = updatedUser.id!!
        val token = jwtService.generateToken(userId, updatedUser.email)

        return VerifyEmailResponse(
            success = true,
            token = token,
            userId = userId,
            email = updatedUser.email,
            displayName = updatedUser.displayName
        )
    }

    suspend fun resendVerificationCode(request: ResendVerificationCodeRequest): ResendVerificationCodeResponse {
        val user = userRepository.findByEmail(request.email)

        if (user != null && !user.emailVerified) {
            sendVerificationCode(user.email, user.phoneNumber, user.displayName)
        }

        // Always return the same response regardless of whether the email exists or is already
        // verified, so this endpoint can't be used to enumerate registered accounts.
        return ResendVerificationCodeResponse(message = "If that account needs verification, a new code has been sent")
    }

    private suspend fun sendVerificationCode(email: String, phoneNumber: String, displayName: String) {
        val code = (100000..999999).random().toString()
        val hashedCode = passwordEncoder.encode(code)!!

        emailVerificationRepository.deleteByEmail(email)
        emailVerificationRepository.save(
            EmailVerificationEntity(
                email = email,
                codeHash = hashedCode,
                expiresAt = Instant.now().plus(VERIFICATION_CODE_TTL_MINUTES, ChronoUnit.MINUTES)
            )
        )

        smsService.sendVerificationCode(
            to = phoneNumber,
            displayName = displayName.ifBlank { email },
            code = code
        )
    }

    private fun UserEntity.toProfileDto() = UserProfileDto(
        id = id!!,
        email = email,
        displayName = displayName,
        createdAt = createdAt
    )
}

private fun String?.orDefaultDisplayName(email: String): String =
    this?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")