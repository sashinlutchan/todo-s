package com.todo.to_do.data.remote.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class TodoDto(
    val id: String,
    val userId: String,
    val title: String,
    val description: String? = null,
    val dueDate: Instant? = null,
    val priority: String,
    val priorityRank: Int = 0,
    val category: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class TodoRequestDto(
    val title: String,
    val description: String? = null,
    val dueDate: Instant? = null,
    val priority: String,
    val priorityRank: Int = 0,
    val category: String? = null
)

@Serializable
data class ReorderRequestDto(
    val orderedIds: List<String>
)

