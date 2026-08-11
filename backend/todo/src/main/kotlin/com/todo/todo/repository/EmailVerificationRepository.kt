package com.todo.todo.repository

import com.todo.todo.entity.EmailVerificationEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface EmailVerificationRepository : CoroutineCrudRepository<EmailVerificationEntity, String> {
    suspend fun findFirstByEmailOrderByCreatedAtDesc(email: String): EmailVerificationEntity?
    suspend fun deleteByEmail(email: String)
}
