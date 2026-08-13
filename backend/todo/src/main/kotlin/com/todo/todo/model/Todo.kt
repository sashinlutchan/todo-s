package com.todo.todo.model

import java.time.Instant

data class Todo(
    val id: String?,
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

