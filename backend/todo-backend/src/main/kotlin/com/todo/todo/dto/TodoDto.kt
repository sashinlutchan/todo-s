package com.todo.todo.dto

import com.todo.todo.model.Priority
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.Instant

data class TodoRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    @field:Size(max = 2000)
    val description: String? = null,
    val dueDate: Instant? = null,
    val priority: Priority = Priority.MEDIUM,
    val priorityRank: Int = 0,
    val category: String? = null
)

data class TodoResponse(
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

data class ReorderRequest(
    @field:NotEmpty
    val orderedIds: List<String>
)