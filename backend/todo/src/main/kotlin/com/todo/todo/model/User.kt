package com.todo.todo.model

data class User(
    val id: String?,
    val email: String,
    val passwordHash: String
)
