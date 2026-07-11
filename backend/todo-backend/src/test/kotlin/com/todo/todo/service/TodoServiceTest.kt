package com.todo.todo.service

import com.todo.todo.dto.TodoRequest
import com.todo.todo.exception.ForbiddenResourceException
import com.todo.todo.exception.TodoNotFoundException
import com.todo.todo.model.Priority
import com.todo.todo.entity.TodoEntity
import com.todo.todo.repository.TodoRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TodoServiceTest {

    private val todoRepository = mockk<TodoRepository>()
    private val syncBroadcastService = mockk<SyncBroadcastService>(relaxed = true)
    private val service = TodoService(todoRepository, syncBroadcastService)

    private val userId = "user-1"

    private fun entity(id: String, owner: String = userId, completed: Boolean = false) = TodoEntity(
        id = id,
        userId = owner,
        title = "Title",
        description = null,
        dueDate = null,
        priority = Priority.MEDIUM.name,
        priorityRank = 0,
        category = null,
        isCompleted = completed,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH
    )

    @Test
    fun `createTodo persists and broadcasts`() = runTest {
        val saved = slot<TodoEntity>()
        coEvery { todoRepository.save(capture(saved)) } answers { saved.captured.copy(id = "todo-1") }

        val result = service.createTodo(userId, TodoRequest(title = "Buy milk"))

        assertEquals("todo-1", result.id)
        assertEquals(userId, result.userId)
        assertFalse(result.isCompleted)
        coVerify { todoRepository.save(any()) }
        coVerify { syncBroadcastService.broadcast(any()) }
    }

    @Test
    fun `getTodo throws when missing`() = runTest {
        coEvery { todoRepository.findById("missing") } returns null
        assertFailsWith<TodoNotFoundException> { service.getTodo(userId, "missing") }
    }

    @Test
    fun `getTodo forbids access to another users todo`() = runTest {
        coEvery { todoRepository.findById("todo-9") } returns entity("todo-9", owner = "someone-else")
        assertFailsWith<ForbiddenResourceException> { service.getTodo(userId, "todo-9") }
    }

    @Test
    fun `toggleComplete flips completion flag`() = runTest {
        coEvery { todoRepository.findById("todo-1") } returns entity("todo-1", completed = false)
        val saved = slot<TodoEntity>()
        coEvery { todoRepository.save(capture(saved)) } answers { saved.captured }

        val result = service.toggleComplete(userId, "todo-1")

        assertTrue(result.isCompleted)
    }

    @Test
    fun `reorder assigns sequential priority ranks`() = runTest {
        coEvery { todoRepository.findById(any()) } answers { entity(firstArg()) }
        val saved = slot<TodoEntity>()
        coEvery { todoRepository.save(capture(saved)) } answers { saved.captured }

        val result = service.reorder(userId, listOf("a", "b", "c"))

        assertEquals(listOf(0, 1, 2), result.map { it.priorityRank })
    }

    @Test
    fun `deleteTodo removes owned todo`() = runTest {
        coEvery { todoRepository.findById("todo-1") } returns entity("todo-1")
        coEvery { todoRepository.deleteById("todo-1") } just Runs

        service.deleteTodo(userId, "todo-1")

        coVerify { todoRepository.deleteById("todo-1") }
    }

    @Test
    fun `listTodos returns mapped domain models`() = runTest {
        every { todoRepository.findByUserIdOrderByPriorityRankAsc(userId) } returns
            flowOf(entity("todo-1"), entity("todo-2"))

        val result = service.listTodos(userId, null, null, null)

        assertEquals(2, result.size)
        assertEquals(listOf("todo-1", "todo-2"), result.map { it.id })
    }
}
