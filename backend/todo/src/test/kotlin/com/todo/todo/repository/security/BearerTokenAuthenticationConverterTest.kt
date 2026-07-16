package com.todo.todo.repository.security

import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import reactor.test.StepVerifier

class BearerTokenAuthenticationConverterTest {

    private val converter = BearerTokenAuthenticationConverter()

    private fun exchangeWithAuthHeader(headerValue: String?): MockServerWebExchange {
        val builder = MockServerHttpRequest.get("/api/v1/todos")
        if (headerValue != null) {
            builder.header(HttpHeaders.AUTHORIZATION, headerValue)
        }
        return MockServerWebExchange.from(builder.build())
    }

    @Test
    fun `extracts the raw token from a well-formed Bearer header`() {
        val exchange = exchangeWithAuthHeader("Bearer a.valid.signed-jwt")

        StepVerifier.create(converter.convert(exchange))
            .assertNext { authentication ->
                val token = authentication as UsernamePasswordAuthenticationToken
                org.junit.jupiter.api.Assertions.assertEquals("a.valid.signed-jwt", token.credentials)
            }
            .verifyComplete()
    }

    @Test
    fun `matches the Bearer scheme case-insensitively`() {
        val exchange = exchangeWithAuthHeader("bearer a.valid.signed-jwt")

        StepVerifier.create(converter.convert(exchange))
            .assertNext { authentication ->
                org.junit.jupiter.api.Assertions.assertEquals("a.valid.signed-jwt", authentication.credentials)
            }
            .verifyComplete()
    }

    @Test
    fun `resolves empty when the Authorization header is missing`() {
        val exchange = exchangeWithAuthHeader(null)

        StepVerifier.create(converter.convert(exchange)).verifyComplete()
    }

    @Test
    fun `resolves empty when the header does not use the Bearer scheme`() {
        val exchange = exchangeWithAuthHeader("Basic ZWxlbmE6c2VjcmV0")

        StepVerifier.create(converter.convert(exchange)).verifyComplete()
    }

    @Test
    fun `resolves empty when the Bearer header carries no token`() {
        val exchange = exchangeWithAuthHeader("Bearer ")

        StepVerifier.create(converter.convert(exchange)).verifyComplete()
    }
}