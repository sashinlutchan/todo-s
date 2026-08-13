package com.todo.todo.config

import com.todo.todo.repository.security.BearerTokenAuthenticationConverter
import com.todo.todo.repository.security.JwtAuthenticationManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers

private val PUBLIC_PATHS = arrayOf(
    "/api/v1/auth/register",
    "/api/v1/auth/login",
    "/api/v1/auth/verify",
    "/api/v1/auth/forgot-password",
    "/api/v1/auth/verify-reset-code",
    "/api/v1/auth/reset-password",
    "/api/v1/auth/verify-email",
    "/api/v1/auth/resend-verification-code",
    "/ws/**",
    "/swagger-ui.html",
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/v3/api-docs",
    "/webjars/**"
)

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val jwtAuthenticationManager: JwtAuthenticationManager
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        val authenticationWebFilter = AuthenticationWebFilter(jwtAuthenticationManager)
        authenticationWebFilter.setServerAuthenticationConverter(BearerTokenAuthenticationConverter())
        authenticationWebFilter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance())
        
        authenticationWebFilter.setRequiresAuthenticationMatcher(
            NegatedServerWebExchangeMatcher(ServerWebExchangeMatchers.pathMatchers(*PUBLIC_PATHS))
        )

        return http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange { exchange ->
                exchange
                    .pathMatchers(*PUBLIC_PATHS).permitAll()
                    .anyExchange().authenticated()
            }
            .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }
}

