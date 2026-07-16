package com.todo.todo.service

import com.todo.todo.dto.AuthRequest
import com.todo.todo.entity.UserEntity
import com.todo.todo.exception.EmailAlreadyInUseException
import com.todo.todo.exception.InvalidCredentialsException
import com.todo.todo.repository.UserRepository
import com.todo.todo.repository.security.JwtService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.assertFailsWith

class AuthServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val jwtService = mockk<JwtService>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val authService = AuthService(userRepository, jwtService, passwordEncoder)

    @Test
    fun `register hashes the password, persists a new user, and returns a signed token`() = runTest {
        val request = AuthRequest(email = "elena.martinez@protonmail.com", password = "Tr0ubad0ur&3xpanse!")
        coEvery { userRepository.findByEmail(request.email) } returns null
        every { passwordEncoder.encode(request.password) } returns "\$2a\$10\$hashedTroubadourExpanse"

        val savedSlot = slot<UserEntity>()
        coEvery { userRepository.save(capture(savedSlot)) } answers {
            savedSlot.captured.copy(id = "68a1f0c2e4b0a1a2b3c40001")
        }
        every { jwtService.generateToken("68a1f0c2e4b0a1a2b3c40001", request.email) } returns "signed.jwt.for-elena"

        val response = authService.register(request)

        assertEquals("68a1f0c2e4b0a1a2b3c40001", response.userId)
        assertEquals(request.email, response.email)
        assertEquals("signed.jwt.for-elena", response.token)
        assertEquals("\$2a\$10\$hashedTroubadourExpanse", savedSlot.captured.passwordHash)
        coVerify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    fun `register rejects an email that is already registered`() = runTest {
        val request = AuthRequest(email = "marcus.chen@outlook.com", password = "correct-horse-battery-staple")
        coEvery { userRepository.findByEmail(request.email) } returns UserEntity(
            id = "68a1f0c2e4b0a1a2b3c40002",
            email = request.email,
            passwordHash = "\$2a\$10\$existingHash"
        )

        assertFailsWith<EmailAlreadyInUseException> { authService.register(request) }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `login issues a token when the password matches the stored hash`() = runTest {
        val request = AuthRequest(email = "elena.martinez@protonmail.com", password = "Tr0ubad0ur&3xpanse!")
        val storedUser = UserEntity(
            id = "68a1f0c2e4b0a1a2b3c40001",
            email = request.email,
            passwordHash = "\$2a\$10\$hashedTroubadourExpanse"
        )
        coEvery { userRepository.findByEmail(request.email) } returns storedUser
        every { passwordEncoder.matches(request.password, storedUser.passwordHash) } returns true
        every { jwtService.generateToken(storedUser.id!!, storedUser.email) } returns "signed.jwt.for-elena"

        val response = authService.login(request)

        assertEquals(storedUser.id, response.userId)
        assertEquals("signed.jwt.for-elena", response.token)
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
            passwordHash = "\$2a\$10\$correctHorseBatteryStapleHash"
        )
        coEvery { userRepository.findByEmail(request.email) } returns storedUser
        every { passwordEncoder.matches(request.password, storedUser.passwordHash) } returns false

        assertFailsWith<InvalidCredentialsException> { authService.login(request) }
    }
}
