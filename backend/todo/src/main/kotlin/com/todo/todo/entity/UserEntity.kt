package com.todo.todo.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "users")
data class UserEntity(
    @Id val id: String? = null,
    @Indexed(unique = true) val email: String,
    val passwordHash: String
)
