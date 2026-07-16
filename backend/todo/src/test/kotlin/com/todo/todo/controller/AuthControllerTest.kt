package com.todo.todo.controller

import com.todo.todo.dto.AuthRequest
import com.todo.todo.dto.AuthResponse
import com.todo.todo.entity.UserEntity
import com.todo.todo.repository.UserRepository
import com.todo.todo.support.MockBeansTestConfig
import com.todo.todo.support.TestFixtures
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
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
 * access is faked (see MockBeansTestConfig).
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
    lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun resetMocks() {
        clearMocks(userRepository)
    }

    @Test
    fun `POST register creates a new account and returns a bearer token`() {
        val request = AuthRequest(email = "priya.desai@fastmail.com", password = "Correct-Horse-Battery-9!")
        coEvery { userRepository.findByEmail(request.email) } returns null
        val savedSlot = slot<UserEntity>()
        coEvery { userRepository.save(capture(savedSlot)) } answers {
            savedSlot.captured.copy(id = "68a1f0c2e4b0a1a2b3c40003")
        }

        val response = webTestClient.post()
            .uri("/api/v1/auth/register")
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody(AuthResponse::class.java)
            .returnResult()
            .responseBody!!

        assertEquals("68a1f0c2e4b0a1a2b3c40003", response.userId)
        assertEquals(request.email, response.email)
        assertEquals(3, response.token.split(".").size, "expected a real three-part signed JWT")
        assertTrue(
            passwordEncoder.matches(request.password, savedSlot.captured.passwordHash),
            "the persisted hash must actually match the submitted password"
        )
        assertTrue(savedSlot.captured.passwordHash != request.password, "the raw password must never be persisted")
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
}