package com.todo.todo.repository.security

import com.todo.todo.exception.InvalidTokenException
import kotlinx.coroutines.reactor.mono
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import reactor.test.StepVerifier

class SecurityUtilsTest {

    @Test
    fun `currentUserId reads the authenticated principal name out of the reactive security context`() {
        val authentication = UsernamePasswordAuthenticationToken(
            "68a1f0c2e4b0a1a2b3c40001",
            "a.valid.signed-jwt",
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )

        val result = mono { currentUserId() }
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))

        StepVerifier.create(result)
            .expectNext("68a1f0c2e4b0a1a2b3c40001")
            .verifyComplete()
    }

    @Test
    fun `currentUserId throws InvalidTokenException when no security context is present`() {
        val result = mono { currentUserId() }

        StepVerifier.create(result)
            .expectError(InvalidTokenException::class.java)
            .verify()
    }

    @Test
    fun `currentUserId throws InvalidTokenException when the authentication is not marked authenticated`() {
        val unauthenticated = UsernamePasswordAuthenticationToken(
            "68a1f0c2e4b0a1a2b3c40002",
            "some-token"
        )

        val result = mono { currentUserId() }
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(unauthenticated))

        StepVerifier.create(result)
            .expectError(InvalidTokenException::class.java)
            .verify()
    }
}
