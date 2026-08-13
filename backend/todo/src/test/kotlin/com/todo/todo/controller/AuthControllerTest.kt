package com.todo.todo.controller

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
import com.todo.todo.repository.EmailVerificationRepository
import com.todo.todo.repository.PasswordResetRepository
import com.todo.todo.repository.UserRepository
import com.todo.todo.repository.security.JwtService
import com.todo.todo.service.sms.SmsService
import com.todo.todo.support.MockBeansTestConfig
import com.todo.todo.support.TestFixtures
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Full-stack test hitting the auth endpoints over real HTTP, through the actual
 * security filter chain, JWT signing, and BCrypt password hashing - only Mongo
 * access and outbound email are faked (see MockBeansTestConfig).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(MockBeansTestConfig::class)
class AuthControllerTest {

    @LocalServerPort
    var port: Int = 0

    private val webTestClient: WebTestClient by lazy {
        WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordResetRepository: PasswordResetRepository

    @Autowired
    lateinit var emailVerificationRepository: EmailVerificationRepository

    @Autowired
    lateinit var smsService: SmsService

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var jwtService: JwtService

    @BeforeEach
    fun resetMocks() {
        clearMocks(userRepository, passwordResetRepository, emailVerificationRepository, smsService)
    }

    @Test
    fun `POST register creates a new unverified account and sends a verification code`() {
        val request = AuthRequest(
            email = "priya.desai@fastmail.com",
            password = "Correct-Horse-Battery-9!",
            phoneNumber = "+15551230003"
        )
        coEvery { userRepository.findByEmail(request.email) } returns null
        val savedSlot = slot<UserEntity>()
        coEvery { userRepository.save(capture(savedSlot)) } answers {
            savedSlot.captured.copy(id = "68a1f0c2e4b0a1a2b3c40003")
        }
        coEvery { emailVerificationRepository.deleteByEmail(request.email) } returns Unit
        val verificationSlot = slot<EmailVerificationEntity>()
        coEvery { emailVerificationRepository.save(capture(verificationSlot)) } answers { verificationSlot.captured }

        val response = webTestClient.post()
            .uri("/api/v1/auth/register")
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody(RegisterResponse::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(request.email, response.email)
        assertFalse(savedSlot.captured.emailVerified)
        assertTrue(
            passwordEncoder.matches(request.password, savedSlot.captured.passwordHash),
            "the persisted hash must actually match the submitted password"
        )
        assertTrue(savedSlot.captured.passwordHash != request.password, "the raw password must never be persisted")
        assertEquals(request.email, verificationSlot.captured.email)
    }

    @Test
    fun `POST register returns 409 when the email is already registered`() {
        val request = AuthRequest(email = "elena.martinez@protonmail.com", password = "whatever-1234")
        coEvery { userRepository.findByEmail(request.email) } returns TestFixtures.elenaMartinez()

        webTestClient.post()
            .uri("/api/v1/auth/register")
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody()
            .jsonPath("$.message").isEqualTo("Email 'elena.martinez@protonmail.com' is already in use")
            .jsonPath("$.status").isEqualTo(409)
    }

    @Test
    fun `POST register returns 400 when no phone number is submitted`() {
        val request = AuthRequest(email = "no.phone@example.com", password = "whatever-1234")
        coEvery { userRepository.findByEmail(request.email) } returns null

        webTestClient.post()
            .uri("/api/v1/auth/register")
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").isEqualTo("A phone number is required to register")
    }

    @Test
    fun `POST register returns 400 when the phone number has no country code`() {
        val request = AuthRequest(
            email = "local.format@example.com",
            password = "whatever-1234",
            phoneNumber = "5551230001"
        )
        coEvery { userRepository.findByEmail(request.email) } returns null

        webTestClient.post()
            .uri("/api/v1/auth/register")
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").isEqualTo("Phone number must be in international format with a country code, e.g. +15551234567")
    }

    @Test
    fun `POST login returns a bearer token when the password matches the stored hash`() {
        val password = "Tr0ubad0ur&3xpanse!"
        val storedUser = TestFixtures.elenaMartinez(passwordHash = passwordEncoder.encode(password)!!)
        coEvery { userRepository.findByEmail(storedUser.email) } returns storedUser

        val response = webTestClient.post()
            .uri("/api/v1/auth/login")
            .bodyValue(AuthRequest(storedUser.email, password))
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthResponse::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(storedUser.id, response.userId)
        assertEquals(storedUser.email, response.email)
        assertEquals(storedUser.displayName, response.displayName)
        assertEquals(3, response.token.split(".").size)
    }

    @Test
    fun `POST login returns 401 for an unknown email`() {
        coEvery { userRepository.findByEmail("nobody.here@example.com") } returns null

        webTestClient.post()
            .uri("/api/v1/auth/login")
            .bodyValue(AuthRequest("nobody.here@example.com", "whatever-1234"))
            .exchange()
            .expectStatus().isUnauthorized
            .expectBody()
            .jsonPath("$.message").isEqualTo("Invalid email or password")
    }

    @Test
    fun `POST login returns 401 for a mismatched password`() {
        val storedUser = TestFixtures.marcusChen(passwordHash = passwordEncoder.encode("correct-horse-battery-staple")!!)
        coEvery { userRepository.findByEmail(storedUser.email) } returns storedUser

        webTestClient.post()
            .uri("/api/v1/auth/login")
            .bodyValue(AuthRequest(storedUser.email, "wrong-password"))
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `POST login returns 403 for an account that has not verified its email`() {
        val password = "Tr0ubad0ur&3xpanse!"
        val storedUser = TestFixtures.elenaMartinez(
            passwordHash = passwordEncoder.encode(password)!!,
            emailVerified = false
        )
        coEvery { userRepository.findByEmail(storedUser.email) } returns storedUser

        webTestClient.post()
            .uri("/api/v1/auth/login")
            .bodyValue(AuthRequest(storedUser.email, password))
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `GET verify returns the profile for a currently valid token`() {
        val storedUser = TestFixtures.elenaMartinez()
        val token = jwtService.generateToken(storedUser.id!!, storedUser.email)
        coEvery { userRepository.findById(storedUser.id!!) } returns storedUser

        val response = webTestClient.get()
            .uri("/api/v1/auth/verify")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody(VerifyTokenResponse::class.java)
            .returnResult()
            .responseBody!!

        assertTrue(response.valid)
        assertEquals(storedUser.email, response.user?.email)
        assertEquals(storedUser.displayName, response.user?.displayName)
    }

    @Test
    fun `GET verify returns 401 with TOKEN_INVALID for a forged token`() {
        webTestClient.get()
            .uri("/api/v1/auth/verify")
            .header("Authorization", "Bearer not-a-real-token")
            .exchange()
            .expectStatus().isUnauthorized
            .expectBody()
            .jsonPath("$.valid").isEqualTo(false)
            .jsonPath("$.reason").isEqualTo("TOKEN_INVALID")
    }

    @Test
    fun `GET verify returns 401 with TOKEN_INVALID when no Authorization header is sent`() {
        webTestClient.get()
            .uri("/api/v1/auth/verify")
            .exchange()
            .expectStatus().isUnauthorized
            .expectBody()
            .jsonPath("$.reason").isEqualTo("TOKEN_INVALID")
    }

    @Test
    fun `GET profile returns the authenticated user's profile`() {
        val storedUser = TestFixtures.marcusChen()
        val token = jwtService.generateToken(storedUser.id!!, storedUser.email)
        coEvery { userRepository.findById(storedUser.id!!) } returns storedUser

        val response = webTestClient.get()
            .uri("/api/v1/auth/profile")
            .header("Authorization", "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody(UserProfileDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(storedUser.email, response.email)
        assertEquals(storedUser.displayName, response.displayName)
    }

    @Test
    fun `GET profile is rejected without a bearer token`() {
        webTestClient.get()
            .uri("/api/v1/auth/profile")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `POST forgot-password always returns 200 whether or not the email is registered`() {
        coEvery { userRepository.findByEmail("nobody.here@example.com") } returns null

        webTestClient.post()
            .uri("/api/v1/auth/forgot-password")
            .bodyValue(ForgotPasswordRequest(email = "nobody.here@example.com"))
            .exchange()
            .expectStatus().isOk
            .expectBody(ForgotPasswordResponse::class.java)
            .returnResult()
            .responseBody!!
            .let { assertEquals("If that email exists, a reset code has been texted to the phone on file", it.message) }
    }

    @Test
    fun `POST forgot-password sends an email when the address is registered`() {
        val storedUser = TestFixtures.elenaMartinez()
        coEvery { userRepository.findByEmail(storedUser.email) } returns storedUser
        coEvery { passwordResetRepository.deleteByEmail(storedUser.email) } returns Unit
        val savedSlot = slot<PasswordResetEntity>()
        coEvery { passwordResetRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        webTestClient.post()
            .uri("/api/v1/auth/forgot-password")
            .bodyValue(ForgotPasswordRequest(email = storedUser.email))
            .exchange()
            .expectStatus().isOk

        assertEquals(storedUser.email, savedSlot.captured.email)
    }

    @Test
    fun `POST verify-reset-code returns 400 for an unknown email`() {
        coEvery { passwordResetRepository.findFirstByEmailOrderByCreatedAtDesc(any()) } returns null

        webTestClient.post()
            .uri("/api/v1/auth/verify-reset-code")
            .bodyValue(VerifyResetCodeRequest(email = "nobody.here@example.com", code = "000000"))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(VerifyResetCodeResponse::class.java)
            .returnResult()
            .responseBody!!
            .let {
                assertFalse(it.valid)
                assertEquals("EMAIL_NOT_FOUND", it.reason)
            }
    }

    @Test
    fun `POST verify-reset-code returns 200 with a reset token for a correct, unexpired code`() {
        val email = "elena.martinez@protonmail.com"
        val record = TestFixtures.passwordResetRecord(email = email, codeHash = passwordEncoder.encode("483920")!!)
        coEvery { passwordResetRepository.findFirstByEmailOrderByCreatedAtDesc(email) } returns record
        coEvery { passwordResetRepository.save(any()) } answers { firstArg() }

        val response = webTestClient.post()
            .uri("/api/v1/auth/verify-reset-code")
            .bodyValue(VerifyResetCodeRequest(email = email, code = "483920"))
            .exchange()
            .expectStatus().isOk
            .expectBody(VerifyResetCodeResponse::class.java)
            .returnResult()
            .responseBody!!

        assertTrue(response.valid)
        assertTrue(response.resetToken!!.split(".").size == 3)
    }

    @Test
    fun `POST reset-password returns 400 for an expired reset token`() {
        val response = webTestClient.post()
            .uri("/api/v1/auth/reset-password")
            .bodyValue(ResetPasswordRequest(resetToken = "not-a-real-reset-token", newPassword = "NewSecureP@ss123"))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ResetPasswordResponse::class.java)
            .returnResult()
            .responseBody!!

        assertFalse(response.success)
        assertEquals("RESET_TOKEN_INVALID", response.reason)
    }

    @Test
    fun `full forgot-password round trip issues a working reset token that updates the password`() {
        val email = "marcus.chen@outlook.com"
        val storedUser = TestFixtures.marcusChen()
        coEvery { passwordResetRepository.findFirstByEmailOrderByCreatedAtDesc(email) } returns
            TestFixtures.passwordResetRecord(email = email, codeHash = passwordEncoder.encode("112233")!!)
        val savedRecordSlot = slot<PasswordResetEntity>()
        coEvery { passwordResetRepository.save(capture(savedRecordSlot)) } answers { savedRecordSlot.captured }
        coEvery { userRepository.findByEmail(email) } returns storedUser
        val savedUserSlot = slot<UserEntity>()
        coEvery { userRepository.save(capture(savedUserSlot)) } answers { savedUserSlot.captured }
        coEvery { passwordResetRepository.deleteByEmail(email) } returns Unit

        val verifyResponse = webTestClient.post()
            .uri("/api/v1/auth/verify-reset-code")
            .bodyValue(VerifyResetCodeRequest(email = email, code = "112233"))
            .exchange()
            .expectStatus().isOk
            .expectBody(VerifyResetCodeResponse::class.java)
            .returnResult()
            .responseBody!!

        // verifyResetCode persisted the record as verified=true; wire that into the next lookup.
        coEvery { passwordResetRepository.findFirstByEmailOrderByCreatedAtDesc(email) } returns savedRecordSlot.captured

        val resetResponse = webTestClient.post()
            .uri("/api/v1/auth/reset-password")
            .bodyValue(ResetPasswordRequest(resetToken = verifyResponse.resetToken!!, newPassword = "NewSecureP@ss123"))
            .exchange()
            .expectStatus().isOk
            .expectBody(ResetPasswordResponse::class.java)
            .returnResult()
            .responseBody!!

        assertTrue(resetResponse.success)
        assertTrue(passwordEncoder.matches("NewSecureP@ss123", savedUserSlot.captured.passwordHash))
    }

    @Test
    fun `POST verify-email returns 400 for an unknown email`() {
        coEvery { emailVerificationRepository.findFirstByEmailOrderByCreatedAtDesc(any()) } returns null

        webTestClient.post()
            .uri("/api/v1/auth/verify-email")
            .bodyValue(VerifyEmailRequest(email = "nobody.here@example.com", code = "000000"))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(VerifyEmailResponse::class.java)
            .returnResult()
            .responseBody!!
            .let {
                assertFalse(it.success)
                assertEquals("EMAIL_NOT_FOUND", it.reason)
            }
    }

    @Test
    fun `POST verify-email marks the account verified and returns a bearer token for a correct code`() {
        val storedUser = TestFixtures.elenaMartinez(emailVerified = false)
        val record = EmailVerificationEntity(
            id = "68a1f0c2e4b0a1a2b3c70001",
            email = storedUser.email,
            codeHash = passwordEncoder.encode("483920")!!,
            expiresAt = java.time.Instant.now().plus(15, java.time.temporal.ChronoUnit.MINUTES)
        )
        coEvery { emailVerificationRepository.findFirstByEmailOrderByCreatedAtDesc(storedUser.email) } returns record
        coEvery { userRepository.findByEmail(storedUser.email) } returns storedUser
        val savedUserSlot = slot<UserEntity>()
        coEvery { userRepository.save(capture(savedUserSlot)) } answers { savedUserSlot.captured }
        coEvery { emailVerificationRepository.deleteByEmail(storedUser.email) } returns Unit

        val response = webTestClient.post()
            .uri("/api/v1/auth/verify-email")
            .bodyValue(VerifyEmailRequest(email = storedUser.email, code = "483920"))
            .exchange()
            .expectStatus().isOk
            .expectBody(VerifyEmailResponse::class.java)
            .returnResult()
            .responseBody!!

        assertTrue(response.success)
        assertEquals(3, response.token!!.split(".").size, "expected a real three-part signed JWT")
        assertTrue(savedUserSlot.captured.emailVerified)
    }

    @Test
    fun `POST resend-verification-code always returns 200 whether or not the email is registered`() {
        coEvery { userRepository.findByEmail("nobody.here@example.com") } returns null

        webTestClient.post()
            .uri("/api/v1/auth/resend-verification-code")
            .bodyValue(ResendVerificationCodeRequest(email = "nobody.here@example.com"))
            .exchange()
            .expectStatus().isOk
            .expectBody(ResendVerificationCodeResponse::class.java)
            .returnResult()
            .responseBody!!
            .let { assertEquals("If that account needs verification, a new code has been sent", it.message) }
    }

    @Test
    fun `full signup verification round trip issues a working bearer token`() {
        val storedUser = TestFixtures.marcusChen(emailVerified = false)
        coEvery { userRepository.findByEmail(storedUser.email) } returns storedUser
        coEvery { emailVerificationRepository.findFirstByEmailOrderByCreatedAtDesc(storedUser.email) } returns
            TestFixtures.emailVerificationRecord(email = storedUser.email, codeHash = passwordEncoder.encode("112233")!!)
        val savedUserSlot = slot<UserEntity>()
        coEvery { userRepository.save(capture(savedUserSlot)) } answers { savedUserSlot.captured }
        coEvery { emailVerificationRepository.deleteByEmail(storedUser.email) } returns Unit

        val verifyResponse = webTestClient.post()
            .uri("/api/v1/auth/verify-email")
            .bodyValue(VerifyEmailRequest(email = storedUser.email, code = "112233"))
            .exchange()
            .expectStatus().isOk
            .expectBody(VerifyEmailResponse::class.java)
            .returnResult()
            .responseBody!!

        assertTrue(verifyResponse.success)

        coEvery { userRepository.findById(storedUser.id!!) } returns savedUserSlot.captured

        webTestClient.get()
            .uri("/api/v1/auth/verify")
            .header("Authorization", "Bearer ${verifyResponse.token}")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.valid").isEqualTo(true)
    }
}