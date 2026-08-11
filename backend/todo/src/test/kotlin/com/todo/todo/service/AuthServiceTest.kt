package com.todo.todo.service

import com.todo.todo.dto.AuthRequest
import com.todo.todo.dto.ForgotPasswordRequest
import com.todo.todo.dto.ResendVerificationCodeRequest
import com.todo.todo.dto.ResetPasswordRequest
import com.todo.todo.dto.VerifyEmailRequest
import com.todo.todo.dto.VerifyResetCodeRequest
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
import com.todo.todo.repository.security.JwtPrincipal
import com.todo.todo.repository.security.JwtService
import com.todo.todo.repository.security.ResetTokenValidation
import com.todo.todo.repository.security.TokenValidation
import com.todo.todo.service.sms.SmsService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertFailsWith

class AuthServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordResetRepository = mockk<PasswordResetRepository>()
    private val emailVerificationRepository = mockk<EmailVerificationRepository>()
    private val jwtService = mockk<JwtService>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val smsService = mockk<SmsService>(relaxed = true)
    private val authService = AuthService(
        userRepository,
        passwordResetRepository,
        emailVerificationRepository,
        jwtService,
        passwordEncoder,
        smsService
    )

    @Test
    fun `register hashes the password, persists an unverified user, and sends a verification code`() = runTest {
        val request = AuthRequest(
            email = "elena.martinez@protonmail.com",
            password = "Tr0ubad0ur&3xpanse!",
            phoneNumber = "+15551230001"
        )
        coEvery { userRepository.findByEmail(request.email) } returns null
        every { passwordEncoder.encode(any<String>()) } returns "\$2a\$10\$hashedOtp"
        every { passwordEncoder.encode(request.password) } returns "\$2a\$10\$hashedTroubadourExpanse"

        val savedSlot = slot<UserEntity>()
        coEvery { userRepository.save(capture(savedSlot)) } answers {
            savedSlot.captured.copy(id = "68a1f0c2e4b0a1a2b3c40001")
        }
        coEvery { emailVerificationRepository.deleteByEmail(request.email) } returns Unit
        coEvery { emailVerificationRepository.save(any()) } answers { firstArg() }

        val response = authService.register(request)

        assertEquals(request.email, response.email)
        assertEquals("\$2a\$10\$hashedTroubadourExpanse", savedSlot.captured.passwordHash)
        assertEquals(request.phoneNumber, savedSlot.captured.phoneNumber)
        assertFalse(savedSlot.captured.emailVerified)
        coVerify(exactly = 1) { userRepository.save(any()) }
        coVerify(exactly = 1) { smsService.sendVerificationCode(request.phoneNumber!!, "elena.martinez", any()) }
    }

    @Test
    fun `register uses the submitted displayName when provided`() = runTest {
        val request = AuthRequest(
            email = "elena.martinez@protonmail.com",
            password = "Tr0ubad0ur&3xpanse!",
            displayName = "Elena",
            phoneNumber = "+15551230001"
        )
        coEvery { userRepository.findByEmail(request.email) } returns null
        every { passwordEncoder.encode(any<String>()) } returns "\$2a\$10\$hashedTroubadourExpanse"
        val savedSlot = slot<UserEntity>()
        coEvery { userRepository.save(capture(savedSlot)) } answers {
            savedSlot.captured.copy(id = "68a1f0c2e4b0a1a2b3c40001")
        }
        coEvery { emailVerificationRepository.deleteByEmail(request.email) } returns Unit
        coEvery { emailVerificationRepository.save(any()) } answers { firstArg() }

        authService.register(request)

        assertEquals("Elena", savedSlot.captured.displayName)
    }

    @Test
    fun `register rejects an email that is already registered`() = runTest {
        val request = AuthRequest(
            email = "marcus.chen@outlook.com",
            password = "correct-horse-battery-staple",
            phoneNumber = "+15551230002"
        )
        coEvery { userRepository.findByEmail(request.email) } returns UserEntity(
            id = "68a1f0c2e4b0a1a2b3c40002",
            email = request.email,
            passwordHash = "\$2a\$10\$existingHash"
        )

        assertFailsWith<EmailAlreadyInUseException> { authService.register(request) }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `register rejects a missing phone number`() = runTest {
        val request = AuthRequest(email = "no.phone@example.com", password = "correct-horse-battery-staple")
        coEvery { userRepository.findByEmail(request.email) } returns null

        assertFailsWith<MissingPhoneNumberException> { authService.register(request) }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `register rejects a phone number missing a country code`() = runTest {
        val request = AuthRequest(
            email = "local.format@example.com",
            password = "correct-horse-battery-staple",
            phoneNumber = "5551230001"
        )
        coEvery { userRepository.findByEmail(request.email) } returns null

        assertFailsWith<InvalidPhoneNumberException> { authService.register(request) }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `register rejects a phone number containing non-digit characters`() = runTest {
        val request = AuthRequest(
            email = "formatted.number@example.com",
            password = "correct-horse-battery-staple",
            phoneNumber = "+1 (555) 123-0001"
        )
        coEvery { userRepository.findByEmail(request.email) } returns null

        assertFailsWith<InvalidPhoneNumberException> { authService.register(request) }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `register rejects a phone number that is too short to be a real MSISDN`() = runTest {
        val request = AuthRequest(
            email = "too.short@example.com",
            password = "correct-horse-battery-staple",
            phoneNumber = "+1555"
        )
        coEvery { userRepository.findByEmail(request.email) } returns null

        assertFailsWith<InvalidPhoneNumberException> { authService.register(request) }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `login issues a token when the password matches the stored hash`() = runTest {
        val request = AuthRequest(email = "elena.martinez@protonmail.com", password = "Tr0ubad0ur&3xpanse!")
        val storedUser = UserEntity(
            id = "68a1f0c2e4b0a1a2b3c40001",
            email = request.email,
            passwordHash = "\$2a\$10\$hashedTroubadourExpanse",
            displayName = "Elena Martinez",
            emailVerified = true
        )
        coEvery { userRepository.findByEmail(request.email) } returns storedUser
        every { passwordEncoder.matches(request.password, storedUser.passwordHash) } returns true
        every { jwtService.generateToken(storedUser.id!!, storedUser.email) } returns "signed.jwt.for-elena"

        val response = authService.login(request)

        assertEquals(storedUser.id, response.userId)
        assertEquals("signed.jwt.for-elena", response.token)
        assertEquals("Elena Martinez", response.displayName)
    }

    @Test
    fun `login rejects an unknown email`() = runTest {
        val request = AuthRequest(email = "nobody.here@example.com", password = "whatever-1234")
        coEvery { userRepository.findByEmail(request.email) } returns null

        assertFailsWith<InvalidCredentialsException> { authService.login(request) }
    }

    @Test
    fun `login rejects a mismatched password without revealing which field was wrong`() = runTest {
        val request = AuthRequest(email = "marcus.chen@outlook.com", password = "wrong-password")
        val storedUser = UserEntity(
            id = "68a1f0c2e4b0a1a2b3c40002",
            email = request.email,
            passwordHash = "\$2a\$10\$correctHorseBatteryStapleHash",
            emailVerified = true
        )
        coEvery { userRepository.findByEmail(request.email) } returns storedUser
        every { passwordEncoder.matches(request.password, storedUser.passwordHash) } returns false

        assertFailsWith<InvalidCredentialsException> { authService.login(request) }
    }

    @Test
    fun `login rejects a correct password for an unverified account`() = runTest {
        val request = AuthRequest(email = "elena.martinez@protonmail.com", password = "Tr0ubad0ur&3xpanse!")
        val storedUser = UserEntity(
            id = "68a1f0c2e4b0a1a2b3c40001",
            email = request.email,
            passwordHash = "\$2a\$10\$hashedTroubadourExpanse",
            emailVerified = false
        )
        coEvery { userRepository.findByEmail(request.email) } returns storedUser
        every { passwordEncoder.matches(request.password, storedUser.passwordHash) } returns true

        assertFailsWith<EmailNotVerifiedException> { authService.login(request) }
    }

    @Test
    fun `verifyToken returns the user profile when the token is valid`() = runTest {
        val expiresAt = Instant.parse("2026-07-29T10:00:00Z")
        val storedUser = UserEntity(
            id = "68a1f0c2e4b0a1a2b3c40001",
            email = "elena.martinez@protonmail.com",
            passwordHash = "irrelevant",
            displayName = "Elena Martinez",
            createdAt = Instant.parse("2026-01-15T08:30:00Z")
        )
        every { jwtService.validateToken("valid.jwt") } returns
            TokenValidation.Valid(JwtPrincipal(storedUser.id!!, storedUser.email), expiresAt)
        coEvery { userRepository.findById(storedUser.id!!) } returns storedUser

        val response = authService.verifyToken("valid.jwt")

        assertTrue(response.valid)
        assertEquals(storedUser.email, response.user?.email)
        assertEquals(storedUser.displayName, response.user?.displayName)
        assertEquals(expiresAt, response.tokenExpiresAt)
    }

    @Test
    fun `verifyToken reports TOKEN_EXPIRED for an expired token`() = runTest {
        every { jwtService.validateToken("expired.jwt") } returns TokenValidation.Expired

        val response = authService.verifyToken("expired.jwt")

        assertFalse(response.valid)
        assertEquals("TOKEN_EXPIRED", response.reason)
        assertNull(response.user)
    }

    @Test
    fun `verifyToken reports TOKEN_INVALID for a malformed or forged token`() = runTest {
        every { jwtService.validateToken("garbage") } returns TokenValidation.Invalid

        val response = authService.verifyToken("garbage")

        assertFalse(response.valid)
        assertEquals("TOKEN_INVALID", response.reason)
    }

    @Test
    fun `verifyToken reports USER_NOT_FOUND when the token is valid but the account was deleted`() = runTest {
        every { jwtService.validateToken("valid.jwt") } returns
            TokenValidation.Valid(JwtPrincipal("68a1f0c2e4b0a1a2b3c49999", "gone@example.com"), Instant.now())
        coEvery { userRepository.findById("68a1f0c2e4b0a1a2b3c49999") } returns null

        val response = authService.verifyToken("valid.jwt")

        assertFalse(response.valid)
        assertEquals("USER_NOT_FOUND", response.reason)
    }

    @Test
    fun `getProfile returns the profile for an existing user`() = runTest {
        val storedUser = UserEntity(
            id = "68a1f0c2e4b0a1a2b3c40001",
            email = "elena.martinez@protonmail.com",
            passwordHash = "irrelevant",
            displayName = "Elena Martinez"
        )
        coEvery { userRepository.findById(storedUser.id!!) } returns storedUser

        val profile = authService.getProfile(storedUser.id!!)

        assertEquals(storedUser.email, profile.email)
        assertEquals(storedUser.displayName, profile.displayName)
    }

    @Test
    fun `getProfile throws when the user no longer exists`() = runTest {
        coEvery { userRepository.findById("missing-id") } returns null

        assertFailsWith<UserNotFoundException> { authService.getProfile("missing-id") }
    }

    @Test
    fun `sendForgotPasswordCode texts a code and stores it hashed when the email is registered`() = runTest {
        val request = ForgotPasswordRequest(email = "elena.martinez@protonmail.com")
        val storedUser = UserEntity(
            id = "68a1f0c2e4b0a1a2b3c40001",
            email = request.email,
            passwordHash = "irrelevant",
            phoneNumber = "+15551230001",
            displayName = "Elena Martinez"
        )
        coEvery { userRepository.findByEmail(request.email) } returns storedUser
        coEvery { passwordResetRepository.deleteByEmail(request.email) } returns Unit
        every { passwordEncoder.encode(any()) } returns "\$2a\$10\$hashedOtp"
        val savedSlot = slot<PasswordResetEntity>()
        coEvery { passwordResetRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val response = authService.sendForgotPasswordCode(request)

        assertEquals("If that email exists, a reset code has been texted to the phone on file", response.message)
        coVerify(exactly = 1) { passwordResetRepository.deleteByEmail(request.email) }
        coVerify(exactly = 1) { smsService.sendResetCode(storedUser.phoneNumber, storedUser.displayName, any()) }
        assertEquals(request.email, savedSlot.captured.email)
        assertEquals("\$2a\$10\$hashedOtp", savedSlot.captured.codeHash)
    }

    @Test
    fun `sendForgotPasswordCode returns the same response and sends no text for an unknown address`() = runTest {
        val request = ForgotPasswordRequest(email = "nobody.here@example.com")
        coEvery { userRepository.findByEmail(request.email) } returns null

        val response = authService.sendForgotPasswordCode(request)

        assertEquals("If that email exists, a reset code has been texted to the phone on file", response.message)
        coVerify(exactly = 0) { smsService.sendResetCode(any(), any(), any()) }
        coVerify(exactly = 0) { passwordResetRepository.save(any()) }
    }

    @Test
    fun `verifyResetCode issues a reset token when the code matches and has not expired`() = runTest {
        val record = PasswordResetEntity(
            id = "68a1f0c2e4b0a1a2b3c60001",
            email = "elena.martinez@protonmail.com",
            codeHash = "\$2a\$10\$hashedOtp",
            expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES)
        )
        coEvery { passwordResetRepository.findFirstByEmailOrderByCreatedAtDesc(record.email) } returns record
        every { passwordEncoder.matches("483920", record.codeHash) } returns true
        coEvery { passwordResetRepository.save(any()) } answers { firstArg() }
        every { jwtService.generateResetToken(record.email) } returns "short.lived.reset.token"

        val response = authService.verifyResetCode(VerifyResetCodeRequest(email = record.email, code = "483920"))

        assertTrue(response.valid)
        assertEquals("short.lived.reset.token", response.resetToken)
    }

    @Test
    fun `verifyResetCode reports EMAIL_NOT_FOUND when there is no reset record`() = runTest {
        coEvery { passwordResetRepository.findFirstByEmailOrderByCreatedAtDesc(any()) } returns null

        val response = authService.verifyResetCode(VerifyResetCodeRequest(email = "nobody@example.com", code = "000000"))

        assertFalse(response.valid)
        assertEquals("EMAIL_NOT_FOUND", response.reason)
    }

    @Test
    fun `verifyResetCode reports CODE_ALREADY_USED for a consumed record`() = runTest {
        val record = PasswordResetEntity(
            email = "elena.martinez@protonmail.com",
            codeHash = "\$2a\$10\$hashedOtp",
            used = true,
            expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES)
        )
        coEvery { passwordResetRepository.findFirstByEmailOrderByCreatedAtDesc(record.email) } returns record

        val response = authService.verifyResetCode(VerifyResetCodeRequest(email = record.email, code = "483920"))

        assertFalse(response.valid)
        assertEquals("CODE_ALREADY_USED", response.reason)
    }

    @Test
    fun `verifyResetCode reports CODE_EXPIRED once the 15 minute TTL has passed`() = runTest {
        val record = PasswordResetEntity(
            email = "elena.martinez@protonmail.com",
            codeHash = "\$2a\$10\$hashedOtp",
            expiresAt = Instant.now().minus(1, ChronoUnit.MINUTES)
        )
        coEvery { passwordResetRepository.findFirstByEmailOrderByCreatedAtDesc(record.email) } returns record

        val response = authService.verifyResetCode(VerifyResetCodeRequest(email = record.email, code = "483920"))

        assertFalse(response.valid)
        assertEquals("CODE_EXPIRED", response.reason)
    }

    @Test
    fun `verifyResetCode reports CODE_INVALID for a wrong code`() = runTest {
        val record = PasswordResetEntity(
            email = "elena.martinez@protonmail.com",
            codeHash = "\$2a\$10\$hashedOtp",
            expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES)
        )
        coEvery { passwordResetRepository.findFirstByEmailOrderByCreatedAtDesc(record.email) } returns record
        every { passwordEncoder.matches("000000", record.codeHash) } returns false

        val response = authService.verifyResetCode(VerifyResetCodeRequest(email = record.email, code = "000000"))

        assertFalse(response.valid)
        assertEquals("CODE_INVALID", response.reason)
    }

    @Test
    fun `resetPassword updates the password when the reset token and prior verification are valid`() = runTest {
        val email = "elena.martinez@protonmail.com"
        val storedUser = UserEntity(
            id = "68a1f0c2e4b0a1a2b3c40001",
            email = email,
            passwordHash = "\$2a\$10\$oldHash",
            displayName = "Elena Martinez"
        )
        val record = PasswordResetEntity(
            email = email,
            codeHash = "\$2a\$10\$hashedOtp",
            verified = true,
            expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES)
        )
        every { jwtService.validateResetToken("short.lived.reset.token") } returns ResetTokenValidation.Valid(email)
        coEvery { userRepository.findByEmail(email) } returns storedUser
        coEvery { passwordResetRepository.findFirstByEmailOrderByCreatedAtDesc(email) } returns record
        every { passwordEncoder.encode("NewSecureP@ss123") } returns "\$2a\$10\$newHash"
        coEvery { userRepository.save(any()) } answers { firstArg() }
        coEvery { passwordResetRepository.deleteByEmail(email) } returns Unit

        val response = authService.resetPassword(
            ResetPasswordRequest(resetToken = "short.lived.reset.token", newPassword = "NewSecureP@ss123")
        )

        assertTrue(response.success)
        coVerify(exactly = 1) { passwordResetRepository.deleteByEmail(email) }
        coVerify(exactly = 1) {
            userRepository.save(match { it.passwordHash == "\$2a\$10\$newHash" })
        }
    }

    @Test
    fun `resetPassword reports RESET_TOKEN_EXPIRED for an expired reset token`() = runTest {
        every { jwtService.validateResetToken("stale.token") } returns ResetTokenValidation.Expired

        val response = authService.resetPassword(ResetPasswordRequest(resetToken = "stale.token", newPassword = "whatever123"))

        assertFalse(response.success)
        assertEquals("RESET_TOKEN_EXPIRED", response.reason)
    }

    @Test
    fun `resetPassword reports RESET_TOKEN_INVALID for a forged reset token`() = runTest {
        every { jwtService.validateResetToken("forged.token") } returns ResetTokenValidation.Invalid

        val response = authService.resetPassword(ResetPasswordRequest(resetToken = "forged.token", newPassword = "whatever123"))

        assertFalse(response.success)
        assertEquals("RESET_TOKEN_INVALID", response.reason)
    }

    @Test
    fun `resetPassword reports PASSWORD_TOO_SHORT before touching the user record`() = runTest {
        every { jwtService.validateResetToken("short.lived.reset.token") } returns
            ResetTokenValidation.Valid("elena.martinez@protonmail.com")

        val response = authService.resetPassword(
            ResetPasswordRequest(resetToken = "short.lived.reset.token", newPassword = "short")
        )

        assertFalse(response.success)
        assertEquals("PASSWORD_TOO_SHORT", response.reason)
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `resetPassword rejects a reset token whose code was never verified`() = runTest {
        val email = "elena.martinez@protonmail.com"
        every { jwtService.validateResetToken("short.lived.reset.token") } returns ResetTokenValidation.Valid(email)
        coEvery { userRepository.findByEmail(email) } returns UserEntity(
            id = "68a1f0c2e4b0a1a2b3c40001",
            email = email,
            passwordHash = "\$2a\$10\$oldHash"
        )
        coEvery { passwordResetRepository.findFirstByEmailOrderByCreatedAtDesc(email) } returns PasswordResetEntity(
            email = email,
            codeHash = "\$2a\$10\$hashedOtp",
            verified = false,
            expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES)
        )

        val response = authService.resetPassword(
            ResetPasswordRequest(resetToken = "short.lived.reset.token", newPassword = "NewSecureP@ss123")
        )

        assertFalse(response.success)
        assertEquals("RESET_TOKEN_INVALID", response.reason)
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `verifyEmail marks the account verified and issues a token when the code matches and has not expired`() = runTest {
        val storedUser = UserEntity(
            id = "68a1f0c2e4b0a1a2b3c40001",
            email = "elena.martinez@protonmail.com",
            passwordHash = "\$2a\$10\$hashedTroubadourExpanse",
            displayName = "Elena Martinez",
            emailVerified = false
        )
        val record = EmailVerificationEntity(
            id = "68a1f0c2e4b0a1a2b3c70001",
            email = storedUser.email,
            codeHash = "\$2a\$10\$hashedOtp",
            expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES)
        )
        coEvery { emailVerificationRepository.findFirstByEmailOrderByCreatedAtDesc(record.email) } returns record
        every { passwordEncoder.matches("483920", record.codeHash) } returns true
        coEvery { userRepository.findByEmail(storedUser.email) } returns storedUser
        val savedSlot = slot<UserEntity>()
        coEvery { userRepository.save(capture(savedSlot)) } answers { savedSlot.captured }
        coEvery { emailVerificationRepository.deleteByEmail(record.email) } returns Unit
        every { jwtService.generateToken(storedUser.id!!, storedUser.email) } returns "signed.jwt.for-elena"

        val response = authService.verifyEmail(VerifyEmailRequest(email = record.email, code = "483920"))

        assertTrue(response.success)
        assertEquals("signed.jwt.for-elena", response.token)
        assertEquals(storedUser.id, response.userId)
        assertTrue(savedSlot.captured.emailVerified)
        coVerify(exactly = 1) { emailVerificationRepository.deleteByEmail(record.email) }
    }

    @Test
    fun `verifyEmail reports EMAIL_NOT_FOUND when there is no verification record`() = runTest {
        coEvery { emailVerificationRepository.findFirstByEmailOrderByCreatedAtDesc(any()) } returns null

        val response = authService.verifyEmail(VerifyEmailRequest(email = "nobody@example.com", code = "000000"))

        assertFalse(response.success)
        assertEquals("EMAIL_NOT_FOUND", response.reason)
    }

    @Test
    fun `verifyEmail reports CODE_EXPIRED once the TTL has passed`() = runTest {
        val record = EmailVerificationEntity(
            email = "elena.martinez@protonmail.com",
            codeHash = "\$2a\$10\$hashedOtp",
            expiresAt = Instant.now().minus(1, ChronoUnit.MINUTES)
        )
        coEvery { emailVerificationRepository.findFirstByEmailOrderByCreatedAtDesc(record.email) } returns record

        val response = authService.verifyEmail(VerifyEmailRequest(email = record.email, code = "483920"))

        assertFalse(response.success)
        assertEquals("CODE_EXPIRED", response.reason)
    }

    @Test
    fun `verifyEmail reports CODE_INVALID for a wrong code`() = runTest {
        val record = EmailVerificationEntity(
            email = "elena.martinez@protonmail.com",
            codeHash = "\$2a\$10\$hashedOtp",
            expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES)
        )
        coEvery { emailVerificationRepository.findFirstByEmailOrderByCreatedAtDesc(record.email) } returns record
        every { passwordEncoder.matches("000000", record.codeHash) } returns false

        val response = authService.verifyEmail(VerifyEmailRequest(email = record.email, code = "000000"))

        assertFalse(response.success)
        assertEquals("CODE_INVALID", response.reason)
    }

    @Test
    fun `resendVerificationCode texts a new code when the account exists and is not yet verified`() = runTest {
        val request = ResendVerificationCodeRequest(email = "elena.martinez@protonmail.com")
        val storedUser = UserEntity(
            id = "68a1f0c2e4b0a1a2b3c40001",
            email = request.email,
            passwordHash = "irrelevant",
            phoneNumber = "+15551230001",
            displayName = "Elena Martinez",
            emailVerified = false
        )
        coEvery { userRepository.findByEmail(request.email) } returns storedUser
        coEvery { emailVerificationRepository.deleteByEmail(request.email) } returns Unit
        every { passwordEncoder.encode(any<String>()) } returns "\$2a\$10\$hashedOtp"
        val savedSlot = slot<EmailVerificationEntity>()
        coEvery { emailVerificationRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val response = authService.resendVerificationCode(request)

        assertEquals("If that account needs verification, a new code has been sent", response.message)
        coVerify(exactly = 1) { smsService.sendVerificationCode(storedUser.phoneNumber, storedUser.displayName, any()) }
        assertEquals(request.email, savedSlot.captured.email)
    }

    @Test
    fun `resendVerificationCode sends no text for an already-verified account`() = runTest {
        val request = ResendVerificationCodeRequest(email = "elena.martinez@protonmail.com")
        coEvery { userRepository.findByEmail(request.email) } returns UserEntity(
            id = "68a1f0c2e4b0a1a2b3c40001",
            email = request.email,
            passwordHash = "irrelevant",
            emailVerified = true
        )

        val response = authService.resendVerificationCode(request)

        assertEquals("If that account needs verification, a new code has been sent", response.message)
        coVerify(exactly = 0) { smsService.sendVerificationCode(any(), any(), any()) }
        coVerify(exactly = 0) { emailVerificationRepository.save(any()) }
    }

    @Test
    fun `resendVerificationCode sends no text for an unknown address`() = runTest {
        val request = ResendVerificationCodeRequest(email = "nobody.here@example.com")
        coEvery { userRepository.findByEmail(request.email) } returns null

        val response = authService.resendVerificationCode(request)

        assertEquals("If that account needs verification, a new code has been sent", response.message)
        coVerify(exactly = 0) { smsService.sendVerificationCode(any(), any(), any()) }
    }
}