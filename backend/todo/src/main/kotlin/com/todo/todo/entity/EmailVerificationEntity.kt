package com.todo.todo.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "email_verifications")
data class EmailVerificationEntity(
    @Id val id: String? = null,
    @Indexed val email: String,
    val codeHash: String,
    @Indexed(expireAfterSeconds = 0) val expiresAt: Instant,
    val createdAt: Instant = Instant.now()
)

