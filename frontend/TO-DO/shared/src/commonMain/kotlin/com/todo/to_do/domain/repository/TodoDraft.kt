package com.todo.to_do.domain.repository

import com.todo.to_do.domain.model.Priority
import kotlinx.datetime.Instant

data class TodoDraft(
    val title: String,
    val description: String? = null,
    val dueDate: Instant? = null,
    val priority: Priority = Priority.MEDIUM,
    val priorityRank: Int = 0,
    val category: String? = null
)
