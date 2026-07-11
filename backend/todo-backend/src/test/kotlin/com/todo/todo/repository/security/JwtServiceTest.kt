package com.todo.todo.repository.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JwtServiceTest {

    private val secret = "test-secret-that-is-definitely-long-enough-for-hs256-signing"
    private val service = JwtService(secret, 60_000)

    @Test
    fun `generated token round-trips to the same principal`() {
        val token = service.generateToken("user-1", "a@b.com")
        val principal = service.parse(token)
        assertEquals("user-1", principal.userId)
        assertEquals("a@b.com", principal.email)
    }

    @Test
    fun `parsing a tampered token fails`() {
        val token = service.generateToken("user-1", "a@b.com")
        assertFailsWith<Exception> { service.parse(token + "tampered") }
    }

    @Test
    fun `expired token is rejected`() {
        val expiredService = JwtService(secret, -1_000)
        val token = expiredService.generateToken("user-1", "a@b.com")
        assertFailsWith<Exception> { service.parse(token) }
    }
}
