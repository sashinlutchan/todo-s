package com.todo.todo.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.Instant

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `EmailAlreadyInUseException maps to 409 CONFLICT with the offending email in the message`() {
        val response = handler.handleDomainException(EmailAlreadyInUseException("elena.martinez@protonmail.com"))

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals(409, response.body!!.status)
        assertEquals("Email 'elena.martinez@protonmail.com' is already in use", response.body!!.message)
    }

    @Test
    fun `InvalidCredentialsException maps to 401 UNAUTHORIZED without leaking which field was wrong`() {
        val response = handler.handleDomainException(InvalidCredentialsException())

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("Invalid email or password", response.body!!.message)
    }

    @Test
    fun `TodoNotFoundException maps to 404 NOT_FOUND with the missing id in the message`() {
        val response = handler.handleDomainException(TodoNotFoundException("68a1f0c2e4b0a1a2b3c5ffff"))

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(404, response.body!!.status)
        assertTrue(response.body!!.message.contains("68a1f0c2e4b0a1a2b3c5ffff"))
    }

    @Test
    fun `ForbiddenResourceException maps to 403 FORBIDDEN`() {
        val response = handler.handleDomainException(ForbiddenResourceException())

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertEquals(403, response.body!!.status)
    }

    @Test
    fun `InvalidTokenException maps to 401 UNAUTHORIZED`() {
        val response = handler.handleDomainException(InvalidTokenException())

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals(401, response.body!!.status)
    }

    @Test
    fun `ErrorResponse timestamp defaults to the moment it is constructed`() {
        val before = Instant.now()
        val response = handler.handleDomainException(TodoNotFoundException("68a1f0c2e4b0a1a2b3c5ffff"))
        val after = Instant.now()

        val timestamp = response.body!!.timestamp
        assertTrue(!timestamp.isBefore(before) && !timestamp.isAfter(after))
    }
}