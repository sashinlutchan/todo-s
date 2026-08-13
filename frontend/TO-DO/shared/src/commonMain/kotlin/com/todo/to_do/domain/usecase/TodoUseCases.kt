package com.todo.to_do.domain.usecase

import com.todo.to_do.domain.model.Todo
import com.todo.to_do.domain.repository.TodoDraft
import com.todo.to_do.domain.repository.TodoRepository
import kotlinx.datetime.Instant

class GetTodosUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(
        from: Instant? = null,
        to: Instant? = null,
        category: String? = null
    ): List<Todo> = repository.getTodos(from, to, category)
}

class GetTodoUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(id: String): Todo = repository.getTodo(id)
}

class CreateTodoUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(draft: TodoDraft): Todo = repository.createTodo(draft)
}

class UpdateTodoUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(id: String, draft: TodoDraft): Todo = repository.updateTodo(id, draft)
}

class ToggleCompleteUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(id: String): Todo = repository.toggleComplete(id)
}

class DeleteTodoUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(id: String) = repository.deleteTodo(id)
}

class ReorderTodosUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(orderedIds: List<String>): List<Todo> = repository.reorder(orderedIds)
}

