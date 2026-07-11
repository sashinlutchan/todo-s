package com.todo.todo.repository.security

import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.test.StepVerifier
import kotlin.test.Test

class BearerTokenAuthenticationConverterTest {

    private val converter = BearerTokenAuthenticationConverter()

    @Test
    fun `returns empty when no authorization header is present`() {
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/todos"))

        StepVerifier.create(converter.convert(exchange)).verifyComplete()
    }

    @Test
    fun `returns empty when the authorization header is not a bearer token`() {
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/todos").header(HttpHeaders.AUTHORIZATION, "Basic abc123")
        )

        StepVerifier.create(converter.convert(exchange)).verifyComplete()
    }

    @Test
    fun `extracts the token from a bearer authorization header`() {
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/todos").header(HttpHeaders.AUTHORIZATION, "Bearer my-jwt")
        )

        StepVerifier.create(converter.convert(exchange))
            .expectNextMatches { it.credentials == "my-jwt" && it.principal == "my-jwt" }
            .verifyComplete()
    }
}
