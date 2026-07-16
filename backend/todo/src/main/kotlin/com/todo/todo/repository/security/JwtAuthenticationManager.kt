package com.todo.todo.repository.security

import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationManager(
    private val jwtService: JwtService
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        val token = authentication.credentials as? String
            ?: return Mono.error(BadCredentialsException("Missing bearer token"))
        val principal = jwtService.parseToken(token)
            ?: return Mono.error(BadCredentialsException("Invalid or expired token"))

        val authenticatedToken = UsernamePasswordAuthenticationToken(
            principal.userId, // principal is the userId (as its name)
            token,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        return Mono.just(authenticatedToken)
    }
}
