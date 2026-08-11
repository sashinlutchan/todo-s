package com.todo.to_do.data.remote.dto

import com.todo.to_do.domain.model.AuthSession
import com.todo.to_do.domain.model.Priority
import com.todo.to_do.domain.model.Todo
import com.todo.to_do.domain.model.UserProfile
import com.todo.to_do.domain.repository.TodoDraft

fun TodoDto.toDomain(): Todo = Todo(
    id = id,
    userId = userId,
    title = title,
    description = description,
    dueDate = dueDate,
    priority = Priority.valueOf(priority),
    priorityRank = priorityRank,
    category = category,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TodoDraft.toRequestDto(): TodoRequestDto = TodoRequestDto(
    title = title,
    description = description,
    dueDate = dueDate,
    priority = priority.name,
    priorityRank = priorityRank,
    category = category
)

fun AuthResponseDto.toDomain(): AuthSession = AuthSession(
    token = token,
    userId = userId,
    email = email,
    displayName = displayName
)

fun UserProfileDto.toDomain(): UserProfile = UserProfile(
    id = id,
    email = email,
    displayName = displayName,
    createdAt = createdAt.toString()
)
