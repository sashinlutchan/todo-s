package com.todo.to_do.domain.repository

import com.todo.to_do.domain.model.Todo
import kotlinx.datetime.Instant

interface TodoRepository {
    suspend fun getTodos(from: Instant?, to: Instant?, category: String?): List<Todo>
    suspend fun getTodo(id: String): Todo
    suspend fun createTodo(draft: TodoDraft): Todo
    suspend fun updateTodo(id: String, draft: TodoDraft): Todo
    suspend fun toggleComplete(id: String): Todo
    suspend fun deleteTodo(id: String)
    suspend fun reorder(orderedIds: List<String>): List<Todo>
}

