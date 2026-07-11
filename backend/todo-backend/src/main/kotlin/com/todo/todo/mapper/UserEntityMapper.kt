package com.todo.todo.mapper

import com.todo.todo.model.User
import com.todo.todo.entity.UserEntity

fun UserEntity.toDomain(): User = User(
    id = id,
    email = email,
    passwordHash = passwordHash,
    createdAt = createdAt
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    email = email,
    passwordHash = passwordHash,
    createdAt = createdAt
)
