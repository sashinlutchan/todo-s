package com.todo.to_do.domain.model

data class AuthSession(
    val token: String,
    val userId: String,
    val email: String
)
