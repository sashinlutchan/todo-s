package com.todo.todo.model

import java.time.Instant

data class User(
    val id: String?,
    val email: String,
    val passwordHash: String,
    val displayName: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

