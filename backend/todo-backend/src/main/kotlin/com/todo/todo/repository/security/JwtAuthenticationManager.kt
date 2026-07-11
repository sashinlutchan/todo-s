package com.todo.todo.repository.security

import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Validates the bearer token carried by the incoming [Authentication] token and, on success,
 * produces an authenticated principal whose name is the userId pulled from the JWT.
 */
@Component
class JwtAuthenticationManager(
    private val jwtService: JwtService
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> = Mono.defer {
        val token = authentication.credentials as? String
            ?: return@defer Mono.empty()
        runCatching { jwtService.parse(token) }
            .map { principal ->
                Mono.just(
                    UsernamePasswordAuthenticationToken(principal.userId, token, emptyList()) as Authentication
                )
            }
            .getOrElse { Mono.empty() }
    }
}
