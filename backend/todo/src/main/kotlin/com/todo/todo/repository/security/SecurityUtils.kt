package com.todo.todo.repository.security

import com.todo.todo.exception.InvalidTokenException
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.security.core.context.ReactiveSecurityContextHolder

suspend fun currentUserId(): String {
    val context = ReactiveSecurityContextHolder.getContext().awaitSingleOrNull()
        ?: throw InvalidTokenException()
    val authentication = context.authentication
        ?: throw InvalidTokenException()
    if (!authentication.isAuthenticated) {
        throw InvalidTokenException()
    }
    return authentication.name
}
