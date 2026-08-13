package com.todo.todo.mapper

import com.todo.todo.entity.TodoEntity
import com.todo.todo.entity.UserEntity
import com.todo.todo.model.Priority
import com.todo.todo.model.Todo
import com.todo.todo.model.User

fun TodoEntity.toDomain(): Todo = Todo(
    id = id,
    userId = userId,
    title = title,
    description = description,
    isCompleted = isCompleted,
    priority = Priority.valueOf(priority),
    category = category,
    dueDate = dueDate,
    priorityRank = priorityRank,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Todo.toEntity(): TodoEntity = TodoEntity(
    id = id,
    userId = userId,
    title = title,
    description = description,
    isCompleted = isCompleted,
    priority = priority.name,
    category = category,
    dueDate = dueDate,
    priorityRank = priorityRank,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun UserEntity.toDomain(): User = User(
    id = id,
    email = email,
    passwordHash = passwordHash,
    displayName = displayName,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    email = email,
    passwordHash = passwordHash,
    displayName = displayName,
    createdAt = createdAt,
    updatedAt = updatedAt
)

