package com.todo.todo.controller

import com.todo.todo.dto.ReorderRequest
import com.todo.todo.dto.TodoRequest
import com.todo.todo.dto.TodoResponse
import com.todo.todo.dto.toResponse
import com.todo.todo.service.TodoService
import com.todo.todo.repository.security.currentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/v1/todos")
class TodoController(
    private val todoService: TodoService
) {

    @GetMapping
    suspend fun list(
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
        @RequestParam(required = false) category: String?
    ): List<TodoResponse> {
        val userId = currentUserId()
        return todoService.listTodos(userId, from, to, category).map { it.toResponse() }
    }

    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: String): TodoResponse =
        todoService.getTodo(currentUserId(), id).toResponse()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(@Valid @RequestBody request: TodoRequest): TodoResponse =
        todoService.createTodo(currentUserId(), request).toResponse()

    @PutMapping("/{id}")
    suspend fun update(@PathVariable id: String, @Valid @RequestBody request: TodoRequest): TodoResponse =
        todoService.updateTodo(currentUserId(), id, request).toResponse()

    @PatchMapping("/{id}/complete")
    suspend fun toggleComplete(@PathVariable id: String): TodoResponse =
        todoService.toggleComplete(currentUserId(), id).toResponse()

    @PatchMapping("/reorder")
    suspend fun reorder(@Valid @RequestBody request: ReorderRequest): List<TodoResponse> =
        todoService.reorder(currentUserId(), request.orderedIds).map { it.toResponse() }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun delete(@PathVariable id: String) {
        todoService.deleteTodo(currentUserId(), id)
    }
}