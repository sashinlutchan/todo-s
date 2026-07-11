package com.todo.to_do.domain.repository

import com.todo.to_do.domain.model.AuthSession

interface AuthRepository {
    suspend fun register(email: String, password: String): AuthSession
    suspend fun login(email: String, password: String): AuthSession
    fun logout()
    fun currentSession(): AuthSession?
}
