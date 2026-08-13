package com.todo.to_do.data.repository

import com.todo.to_do.data.remote.TaskFlowApi
import com.todo.to_do.data.remote.dto.ReorderRequestDto
import com.todo.to_do.data.remote.dto.toDomain
import com.todo.to_do.data.remote.dto.toRequestDto
import com.todo.to_do.domain.model.Todo
import com.todo.to_do.domain.repository.TodoDraft
import com.todo.to_do.domain.repository.TodoRepository
import kotlinx.datetime.Instant

class TodoRepositoryImpl(
    private val api: TaskFlowApi
) : TodoRepository {

    override suspend fun getTodos(from: Instant?, to: Instant?, category: String?): List<Todo> =
        api.getTodos(from, to, category).map { it.toDomain() }

    override suspend fun getTodo(id: String): Todo =
        api.getTodo(id).toDomain()

    override suspend fun createTodo(draft: TodoDraft): Todo =
        api.createTodo(draft.toRequestDto()).toDomain()

    override suspend fun updateTodo(id: String, draft: TodoDraft): Todo =
        api.updateTodo(id, draft.toRequestDto()).toDomain()

    override suspend fun toggleComplete(id: String): Todo =
        api.toggleComplete(id).toDomain()

    override suspend fun deleteTodo(id: String) =
        api.deleteTodo(id)

    override suspend fun reorder(orderedIds: List<String>): List<Todo> =
        api.reorder(ReorderRequestDto(orderedIds)).map { it.toDomain() }
}

