package com.todo.todo.model

import java.time.Instant

data class Todo(
    val id: String?,
    val userId: String,
    val title: String,
    val description: String?,
    val dueDate: Instant?,
    val priority: Priority,
    val priorityRank: Int,
    val category: String?,
    val isCompleted: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class Priority { LOW, MEDIUM, HIGH }