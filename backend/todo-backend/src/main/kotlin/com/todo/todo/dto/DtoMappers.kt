package com.todo.todo.dto

import com.todo.todo.model.Todo

fun Todo.toResponse(): TodoResponse = TodoResponse(
    id = id!!,
    userId = userId,
    title = title,
    description = description,
    dueDate = dueDate,
    priority = priority,
    priorityRank = priorityRank,
    category = category,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)