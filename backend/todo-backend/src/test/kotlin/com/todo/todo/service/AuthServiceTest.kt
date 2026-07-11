package com.todo.todo.service

import com.todo.todo.dto.LoginRequest
import com.todo.todo.dto.RegisterRequest
import com.todo.todo.exception.EmailAlreadyInUseException
import com.todo.todo.exception.InvalidCredentialsException
import com.todo.todo.entity.UserEntity
import com.todo.todo.repository.UserRepository
import com.todo.todo.repository.security.JwtService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AuthServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = BCryptPasswordEncoder()
    private val jwtService = mockk<JwtService>()
    private val service = AuthService(userRepository, passwordEncoder, jwtService)

    init {
        every { jwtService.generateToken(any(), any()) } returns "signed-token"
    }

    @Test
    fun `register hashes password and returns token`() = runTest {
        coEvery { userRepository.existsByEmail("a@b.com") } returns false
        val saved = slot<UserEntity>()
        coEvery { userRepository.save(capture(saved)) } answers { saved.captured.copy(id = "user-1") }

        val result = service.register(RegisterRequest("A@B.com", "password123"))

        assertEquals("signed-token", result.token)
        assertEquals("user-1", result.userId)
        assertEquals("a@b.com", result.email)
    }

    @Test
    fun `register rejects duplicate email`() = runTest {
        coEvery { userRepository.existsByEmail("a@b.com") } returns true
        assertFailsWith<EmailAlreadyInUseException> {
            service.register(RegisterRequest("a@b.com", "password123"))
        }
    }

    @Test
    fun `login succeeds with correct password`() = runTest {
        val hash = passwordEncoder.encode("password123")!!
        coEvery { userRepository.findByEmail("a@b.com") } returns UserEntity(
            id = "user-1", email = "a@b.com", passwordHash = hash, createdAt = Instant.EPOCH
        )

        val result = service.login(LoginRequest("a@b.com", "password123"))

        assertEquals("user-1", result.userId)
    }

    @Test
    fun `login fails with wrong password`() = runTest {
        val hash = passwordEncoder.encode("password123")!!
        coEvery { userRepository.findByEmail("a@b.com") } returns UserEntity(
            id = "user-1", email = "a@b.com", passwordHash = hash, createdAt = Instant.EPOCH
        )
        assertFailsWith<InvalidCredentialsException> {
            service.login(LoginRequest("a@b.com", "wrong-password"))
        }
    }

    @Test
    fun `login fails for unknown email`() = runTest {
        coEvery { userRepository.findByEmail("none@b.com") } returns null
        assertFailsWith<InvalidCredentialsException> {
            service.login(LoginRequest("none@b.com", "password123"))
        }
    }
}
