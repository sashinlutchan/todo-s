package com.todo.todo.service

import com.todo.todo.dto.TodoRequest
import com.todo.todo.entity.TodoEntity
import com.todo.todo.exception.ForbiddenResourceException
import com.todo.todo.model.Priority
import com.todo.todo.repository.TodoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

class TodoServiceTest {

    private val todoRepository = mockk<TodoRepository>()
    private val syncBroadcastService = mockk<SyncBroadcastService>(relaxed = true)
    private val todoService = TodoService(todoRepository, syncBroadcastService)

    @Test
    fun `getTodo should return todo when user is owner`() {
        runBlocking {
            val todoId = "todo123"
            val userId = "user123"
            val entity = TodoEntity(
                id = todoId,
                userId = userId,
                title = "Test Todo",
                description = "Test Desc",
                isCompleted = false,
                priority = "MEDIUM",
                category = "work",
                dueDate = null,
                priorityRank = 0,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            coEvery { todoRepository.findById(todoId) } returns entity

            val result = todoService.getTodo(todoId, userId)

            assertEquals("Test Todo", result.title)
            assertEquals(userId, result.userId)
            coVerify(exactly = 1) { todoRepository.findById(todoId) }
        }
    }

    @Test
    fun `getTodo should throw ForbiddenResourceException when user is not owner`() {
        runBlocking {
            val todoId = "todo123"
            val userId = "user123"
            val entity = TodoEntity(
                id = todoId,
                userId = "differentUser",
                title = "Test Todo",
                description = "Test Desc",
                isCompleted = false,
                priority = "MEDIUM",
                category = "work",
                dueDate = null,
                priorityRank = 0,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            coEvery { todoRepository.findById(todoId) } returns entity

            assertThrows(ForbiddenResourceException::class.java) {
                runBlocking { todoService.getTodo(todoId, userId) }
            }
        }
    }
}
