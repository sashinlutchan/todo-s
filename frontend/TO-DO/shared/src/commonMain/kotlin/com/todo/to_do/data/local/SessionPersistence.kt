package com.todo.to_do.data.local

import com.todo.to_do.domain.model.AuthSession

expect class SessionPersistence {
    fun save(session: AuthSession)
    fun load(): AuthSession?
    fun clear()
}
