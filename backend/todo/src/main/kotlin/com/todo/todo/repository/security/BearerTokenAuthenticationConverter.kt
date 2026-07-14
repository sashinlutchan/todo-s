package com.todo.todo.repository.security

import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

class BearerTokenAuthenticationConverter : ServerAuthenticationConverter {
    override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION) ?: return Mono.empty()
        if (!authHeader.startsWith("Bearer ", ignoreCase = true)) {
            return Mono.empty()
        }
        val token = authHeader.substring(7).trim()
        if (token.isEmpty()) {
            return Mono.empty()
        }
        return Mono.just(UsernamePasswordAuthenticationToken(token, token))
    }
}
