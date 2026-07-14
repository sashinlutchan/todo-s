package com.todo.todo.repository

import com.todo.todo.entity.UserEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface UserRepository : CoroutineCrudRepository<UserEntity, String> {
    suspend fun findByEmail(email: String): UserEntity?
}
