package com.todo.todo.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "todos")
@CompoundIndex(name = "user_dueDate_idx", def = "{'userId': 1, 'dueDate': 1}")
data class TodoEntity(
    @Id
    val id: String? = null,
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
