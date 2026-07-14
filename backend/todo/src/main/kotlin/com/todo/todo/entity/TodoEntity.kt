package com.todo.todo.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "todos")
@CompoundIndexes(
    CompoundIndex(name = "user_due_date_idx", def = "{'userId': 1, 'dueDate': 1}")
)
data class TodoEntity(
    @Id val id: String? = null,
    val userId: String,
    val title: String,
    val description: String?,
    val isCompleted: Boolean,
    val priority: String,
    val category: String?,
    val dueDate: Instant?,
    val priorityRank: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)
