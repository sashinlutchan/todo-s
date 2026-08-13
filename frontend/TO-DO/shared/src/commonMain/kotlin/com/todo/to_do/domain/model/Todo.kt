package com.todo.to_do.domain.model

import kotlinx.datetime.Instant

data class Todo(
    val id: String,
    val userId: String,
    val title: String,
    val description: String?,
    val dueDate: Instant?,
    val priority: Priority,
    val priorityRank: Int,
    val category: String?,
    val isCompleted: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class Priority { LOW, MEDIUM, HIGH }

