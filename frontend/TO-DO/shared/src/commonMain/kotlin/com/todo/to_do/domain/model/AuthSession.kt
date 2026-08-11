package com.todo.to_do.domain.model

data class AuthSession(
    val token: String,
    val userId: String,
    val email: String,
    val displayName: String = "",
    val createdAt: String? = null
)

data class UserProfile(
    val id: String,
    val email: String,
    val displayName: String,
    val createdAt: String
)
