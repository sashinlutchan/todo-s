package com.todo.todo.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "password_resets")
data class PasswordResetEntity(
    @Id val id: String? = null,
    @Indexed val email: String,
    val codeHash: String,
    val used: Boolean = false,
    val verified: Boolean = false,
    @Indexed(expireAfterSeconds = 0) val expiresAt: Instant,
    val createdAt: Instant = Instant.now()
)
