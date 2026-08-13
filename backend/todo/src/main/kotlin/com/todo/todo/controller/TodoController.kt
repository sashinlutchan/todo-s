package com.todo.todo.controller

import com.todo.todo.dto.ReorderRequest
import com.todo.todo.dto.TodoRequest
import com.todo.todo.dto.TodoResponse
import com.todo.todo.dto.toResponse
import com.todo.todo.repository.security.currentUserId
import com.todo.todo.service.TodoService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/v1/todos")
class TodoController(
    private val todoService: TodoService
) {

    @GetMapping
    suspend fun getTodos(
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
        @RequestParam(required = false) category: String?
    ): Flow<TodoResponse> {
        val userId = currentUserId()
        return todoService.getTodos(userId, from, to, category)
            .map { it.toResponse() }
    }

    @GetMapping("/{id}")
    suspend fun getTodo(@PathVariable id: String): TodoResponse {
        val userId = currentUserId()
        return todoService.getTodo(id, userId).toResponse()
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createTodo(@RequestBody request: TodoRequest): TodoResponse {
        val userId = currentUserId()
        return todoService.createTodo(request, userId).toResponse()
    }

    @PutMapping("/{id}")
    suspend fun updateTodo(
        @PathVariable id: String,
        @RequestBody request: TodoRequest
    ): TodoResponse {
        val userId = currentUserId()
        return todoService.updateTodo(id, request, userId).toResponse()
    }

    @PatchMapping("/{id}/complete")
    suspend fun completeTodo(@PathVariable id: String): TodoResponse {
        val userId = currentUserId()
        return todoService.completeTodo(id, userId).toResponse()
    }

    @PatchMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun reorderTodos(@RequestBody request: ReorderRequest) {
        val userId = currentUserId()
        todoService.reorderTodos(request.orderedIds, userId)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun deleteTodo(@PathVariable id: String) {
        val userId = currentUserId()
        todoService.deleteTodo(id, userId)
    }
}

