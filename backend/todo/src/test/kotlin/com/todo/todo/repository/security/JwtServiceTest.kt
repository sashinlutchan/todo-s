package com.todo.todo.repository.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class JwtServiceTest {

    private val jwtService = JwtService(
        secret = "unit-test-signing-secret-must-be-at-least-32-bytes-long",
        expirationMs = 86_400_000L
    )

    @Test
    fun `generateToken then parseToken round-trips the original userId and email`() {
        val token = jwtService.generateToken(
            userId = "68a1f0c2e4b0a1a2b3c40001",
            email = "elena.martinez@protonmail.com"
        )

        val principal = jwtService.parseToken(token)

        assertNotNull(principal)
        assertEquals("68a1f0c2e4b0a1a2b3c40001", principal!!.userId)
        assertEquals("elena.martinez@protonmail.com", principal.email)
    }

    @Test
    fun `parseToken returns null for a token signed with a different secret`() {
        val forgedToken = JwtService(
            secret = "a-completely-different-signing-secret-of-32-plus-bytes",
            expirationMs = 86_400_000L
        ).generateToken("68a1f0c2e4b0a1a2b3c40002", "marcus.chen@outlook.com")

        assertNull(jwtService.parseToken(forgedToken))
    }

    @Test
    fun `parseToken returns null for an already-expired token`() {
        val expiredTokenIssuer = JwtService(
            secret = "unit-test-signing-secret-must-be-at-least-32-bytes-long",
            expirationMs = -1_000L
        )
        val expiredToken = expiredTokenIssuer.generateToken("68a1f0c2e4b0a1a2b3c40001", "elena.martinez@protonmail.com")

        assertNull(jwtService.parseToken(expiredToken))
    }

    @Test
    fun `parseToken returns null for garbage input`() {
        assertNull(jwtService.parseToken("not-a-real-jwt"))
        assertNull(jwtService.parseToken(""))
    }

    @Test
    fun `signing key is padded when the configured secret is shorter than 32 bytes`() {
        val shortSecretService = JwtService(secret = "short-secret", expirationMs = 86_400_000L)

        val token = shortSecretService.generateToken("68a1f0c2e4b0a1a2b3c40003", "priya.desai@fastmail.com")
        val principal = shortSecretService.parseToken(token)

        assertNotNull(principal)
        assertEquals("priya.desai@fastmail.com", principal!!.email)
    }
}