package com.todo.to_do.domain.usecase

import com.todo.to_do.domain.model.AuthSession
import com.todo.to_do.domain.repository.AuthRepository

class RegisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AuthSession =
        repository.register(email, password)
}

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AuthSession =
        repository.login(email, password)
}
