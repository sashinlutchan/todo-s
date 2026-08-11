package com.todo.todo.repository

import com.todo.todo.entity.PasswordResetEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PasswordResetRepository : CoroutineCrudRepository<PasswordResetEntity, String> {
    suspend fun findFirstByEmailOrderByCreatedAtDesc(email: String): PasswordResetEntity?
    suspend fun deleteByEmail(email: String)
}