package com.todo.todo.dto

import com.todo.todo.model.Priority
import com.todo.todo.model.Todo
import java.time.Instant

data class AuthRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val userId: String,
    val email: String
)

data class TodoRequest(
    val title: String,
    val description: String?,
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val category: String?,
    val dueDate: Instant?
)

data class TodoResponse(
    val id: String,
    val userId: String,
    val title: String,
    val description: String?,
    val isCompleted: Boolean,
    val priority: Priority,
    val category: String?,
    val dueDate: Instant?,
    val priorityRank: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class ReorderRequest(
    val orderedIds: List<String>
)

fun Todo.toResponse(): TodoResponse = TodoResponse(
    id = id ?: "",
    userId = userId,
    title = title,
    description = description,
    isCompleted = isCompleted,
    priority = priority,
    category = category,
    dueDate = dueDate,
    priorityRank = priorityRank,
    createdAt = createdAt,
    updatedAt = updatedAt
)

enum class SyncAction {
    CREATED, UPDATED, DELETED
}

data class SyncEvent(
    val userId: String,
    val entity: Todo?,
    val action: SyncAction,
    val entityId: String,
    val timestamp: Instant = Instant.now()
)
