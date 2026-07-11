package com.todo.to_do.data.repository

import com.todo.to_do.data.remote.SessionStore
import com.todo.to_do.data.remote.TaskFlowApi
import com.todo.to_do.data.remote.dto.LoginRequestDto
import com.todo.to_do.data.remote.dto.RegisterRequestDto
import com.todo.to_do.data.remote.dto.toDomain
import com.todo.to_do.domain.model.AuthSession
import com.todo.to_do.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val api: TaskFlowApi,
    private val sessionStore: SessionStore
) : AuthRepository {

    override suspend fun register(email: String, password: String): AuthSession {
        val session = api.register(RegisterRequestDto(email, password)).toDomain()
        sessionStore.update(session)
        return session
    }

    override suspend fun login(email: String, password: String): AuthSession {
        val session = api.login(LoginRequestDto(email, password)).toDomain()
        sessionStore.update(session)
        return session
    }

    override fun logout() = sessionStore.clear()

    override fun currentSession(): AuthSession? = sessionStore.session.value
}
