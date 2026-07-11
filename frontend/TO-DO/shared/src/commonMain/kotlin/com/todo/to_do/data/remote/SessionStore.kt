package com.todo.to_do.data.remote

import com.todo.to_do.data.local.SessionPersistence
import com.todo.to_do.domain.model.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holder for the authenticated session. Seeded from [SessionPersistence] on startup so a login
 * survives process death; the bearer token is read by the Ktor auth interceptor.
 */
class SessionStore(private val persistence: SessionPersistence) {
    private val _session = MutableStateFlow(persistence.load())
    val session: StateFlow<AuthSession?> = _session.asStateFlow()

    val token: String? get() = _session.value?.token

    fun update(session: AuthSession) {
        _session.value = session
        persistence.save(session)
    }

    fun clear() {
        _session.value = null
        persistence.clear()
    }
}
