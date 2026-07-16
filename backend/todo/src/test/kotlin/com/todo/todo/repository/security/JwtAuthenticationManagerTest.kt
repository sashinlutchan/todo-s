package com.todo.todo.repository.security

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import reactor.test.StepVerifier

class JwtAuthenticationManagerTest {

    private val jwtService = mockk<JwtService>()
    private val authenticationManager = JwtAuthenticationManager(jwtService)

    @Test
    fun `authenticate resolves an authenticated token carrying the userId as its principal name`() {
        val token = "a.valid.signed-jwt"
        every { jwtService.parseToken(token) } returns JwtPrincipal(
            userId = "68a1f0c2e4b0a1a2b3c40001",
            email = "elena.martinez@protonmail.com"
        )
        val incoming = UsernamePasswordAuthenticationToken(token, token)

        StepVerifier.create(authenticationManager.authenticate(incoming))
            .assertNext { result ->
                assertEquals("68a1f0c2e4b0a1a2b3c40001", result.name)
                assertTrue(result.isAuthenticated)
                assertTrue(result.authorities.any { it.authority == "ROLE_USER" })
            }
            .verifyComplete()
    }

    @Test
    fun `authenticate errors with bad credentials when the token fails to parse`() {
        val token = "tampered-or-garbage-token"
        every { jwtService.parseToken(token) } returns null
        val incoming = UsernamePasswordAuthenticationToken(token, token)

        StepVerifier.create(authenticationManager.authenticate(incoming))
            .verifyError(BadCredentialsException::class.java)
    }

    @Test
    fun `authenticate errors with bad credentials when credentials are not a String`() {
        val incoming = UsernamePasswordAuthenticationToken("principal", 12345)

        StepVerifier.create(authenticationManager.authenticate(incoming))
            .verifyError(BadCredentialsException::class.java)
    }
}