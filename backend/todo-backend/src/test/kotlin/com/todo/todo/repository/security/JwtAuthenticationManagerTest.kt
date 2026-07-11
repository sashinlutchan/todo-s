package com.todo.todo.repository.security

import io.mockk.every
import io.mockk.mockk
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import reactor.test.StepVerifier
import kotlin.test.Test

class JwtAuthenticationManagerTest {

    private val jwtService = mockk<JwtService>()
    private val manager = JwtAuthenticationManager(jwtService)

    @Test
    fun `authenticates using the userId parsed from a valid token`() {
        every { jwtService.parse("good-token") } returns JwtPrincipal(userId = "user-1", email = "a@b.com")
        val authentication = UsernamePasswordAuthenticationToken("good-token", "good-token")

        StepVerifier.create(manager.authenticate(authentication))
            .expectNextMatches { it.name == "user-1" && it.isAuthenticated }
            .verifyComplete()
    }

    @Test
    fun `rejects a token the JwtService cannot parse`() {
        every { jwtService.parse("bad-token") } throws IllegalArgumentException("malformed")
        val authentication = UsernamePasswordAuthenticationToken("bad-token", "bad-token")

        StepVerifier.create(manager.authenticate(authentication)).verifyComplete()
    }

    @Test
    fun `rejects credentials that are not a string`() {
        val authentication = UsernamePasswordAuthenticationToken("principal", 12345)

        StepVerifier.create(manager.authenticate(authentication)).verifyComplete()
    }
}