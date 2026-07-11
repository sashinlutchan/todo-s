package com.todo.todo.mapper

import com.todo.todo.model.Priority
import com.todo.todo.model.Todo
import com.todo.todo.entity.TodoEntity

fun TodoEntity.toDomain(): Todo = Todo(
    id = id,
    userId = userId,
    title = title,
    description = description,
    dueDate = dueDate,
    priority = Priority.valueOf(priority),
    priorityRank = priorityRank,
    category = category,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Todo.toEntity(): TodoEntity = TodoEntity(
    id = id,
    userId = userId,
    title = title,
    description = description,
    dueDate = dueDate,
    priority = priority.name,
    priorityRank = priorityRank,
    category = category,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)
