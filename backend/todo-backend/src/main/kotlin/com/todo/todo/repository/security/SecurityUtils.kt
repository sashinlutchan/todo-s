package com.todo.todo.repository.security

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.security.core.context.ReactiveSecurityContextHolder

/**
 * userId is always read from the authenticated [org.springframework.security.core.context.SecurityContext],
 * never trusted from the request body.
 */
suspend fun currentUserId(): String {
    val context = ReactiveSecurityContextHolder.getContext().awaitSingleOrNull()
    return context?.authentication?.name
        ?: throw IllegalStateException("No authenticated user in security context")
}
