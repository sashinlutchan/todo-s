package com.todo.todo.service

import com.todo.todo.dto.SyncAction
import com.todo.todo.dto.SyncEvent
import com.todo.todo.dto.TodoRequest
import com.todo.todo.entity.TodoEntity
import com.todo.todo.exception.ForbiddenResourceException
import com.todo.todo.exception.TodoNotFoundException
import com.todo.todo.model.Priority
import com.todo.todo.repository.TodoRepository
import com.todo.todo.support.TestFixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertFailsWith

class TodoServiceTest {

    private val todoRepository = mockk<TodoRepository>()
    private val syncBroadcastService = mockk<SyncBroadcastService>(relaxed = true)
    private val todoService = TodoService(todoRepository, syncBroadcastService)

    private val elenaId = TestFixtures.elenaMartinez().id!!

    @Test
    fun `getTodo returns the todo when the caller owns it`() = runTest {
        val entity = TestFixtures.passportRenewalTodo()
        coEvery { todoRepository.findById(entity.id!!) } returns entity

        val result = todoService.getTodo(entity.id!!, elenaId)

        assertEquals("Renew passport before the Lisbon trip", result.title)
        assertEquals(elenaId, result.userId)
        coVerify(exactly = 1) { todoRepository.findById(entity.id!!) }
    }

    @Test
    fun `getTodo throws ForbiddenResourceException when the caller does not own it`() = runTest {
        val entity = TestFixtures.passportRenewalTodo()

        coEvery { todoRepository.findById(entity.id!!) } returns entity

        assertFailsWith<ForbiddenResourceException> {
            todoService.getTodo(entity.id!!, TestFixtures.marcusChen().id!!)
        }
    }

    @Test
    fun `getTodo throws TodoNotFoundException when no matching document exists`() = runTest {
        coEvery { todoRepository.findById("68a1f0c2e4b0a1a2b3c5ffff") } returns null

        assertFailsWith<TodoNotFoundException> {
            todoService.getTodo("68a1f0c2e4b0a1a2b3c5ffff", elenaId)
        }
    }

    @Test
    fun `createTodo assigns the next priorityRank and broadcasts a CREATED event`() = runTest {
        val request = TodoRequest(
            title = "Prepare slides for the board meeting",
            description = "Cover Q3 revenue and the Lisbon office headcount plan",
            isCompleted = false,
            priority = Priority.HIGH,
            category = "work",
            dueDate = Instant.parse("2026-08-01T09:00:00Z")
        )
        coEvery { todoRepository.countByUserId(elenaId) } returns 2

        val savedSlot = slot<TodoEntity>()
        coEvery { todoRepository.save(capture(savedSlot)) } answers {
            savedSlot.captured.copy(id = "68a1f0c2e4b0a1a2b3c50004")
        }

        val result = todoService.createTodo(request, elenaId)

        assertEquals(2, savedSlot.captured.priorityRank)
        assertEquals("68a1f0c2e4b0a1a2b3c50004", result.id)
        assertEquals(Priority.HIGH, result.priority)

        val eventSlot = slot<SyncEvent>()
        verify(exactly = 1) { syncBroadcastService.broadcast(capture(eventSlot)) }
        assertEquals(SyncAction.CREATED, eventSlot.captured.action)
        assertEquals(elenaId, eventSlot.captured.userId)
        assertEquals("68a1f0c2e4b0a1a2b3c50004", eventSlot.captured.entityId)
    }

    @Test
    fun `updateTodo overwrites fields and broadcasts an UPDATED event`() = runTest {
        val existing = TestFixtures.passportRenewalTodo()
        val request = TodoRequest(
            title = "Renew passport - appointment confirmed for Aug 20",
            description = "Consulate slot booked, bring proof of address",
            isCompleted = false,
            priority = Priority.HIGH,
            category = "personal",
            dueDate = Instant.parse("2026-08-20T10:30:00Z")
        )
        coEvery { todoRepository.findById(existing.id!!) } returns existing
        coEvery { todoRepository.save(any()) } answers { firstArg() }

        val result = todoService.updateTodo(existing.id!!, request, elenaId)

        assertEquals("Renew passport - appointment confirmed for Aug 20", result.title)
        assertEquals(Instant.parse("2026-08-20T10:30:00Z"), result.dueDate)
        verify(exactly = 1) {
            syncBroadcastService.broadcast(match { it.action == SyncAction.UPDATED && it.entityId == existing.id })
        }
    }

    @Test
    fun `updateTodo throws ForbiddenResourceException for another user's todo`() = runTest {
        val existing = TestFixtures.passportRenewalTodo()
        val request = TodoRequest(
            title = "Hijacked title",
            description = null,
            category = null,
            dueDate = null
        )
        coEvery { todoRepository.findById(existing.id!!) } returns existing

        assertFailsWith<ForbiddenResourceException> {
            todoService.updateTodo(existing.id!!, request, TestFixtures.marcusChen().id!!)
        }
        coVerify(exactly = 0) { todoRepository.save(any()) }
    }

    @Test
    fun `updateTodo throws TodoNotFoundException when the todo no longer exists`() = runTest {
        coEvery { todoRepository.findById("68a1f0c2e4b0a1a2b3c5ffff") } returns null
        val request = TodoRequest(title = "Doesn't matter", description = null, category = null, dueDate = null)

        assertFailsWith<TodoNotFoundException> {
            todoService.updateTodo("68a1f0c2e4b0a1a2b3c5ffff", request, elenaId)
        }
    }

    @Test
    fun `completeTodo toggles isCompleted from false to true`() = runTest {
        val existing = TestFixtures.budgetReviewTodo()
        coEvery { todoRepository.findById(existing.id!!) } returns existing
        coEvery { todoRepository.save(any()) } answers { firstArg() }

        val result = todoService.completeTodo(existing.id!!, elenaId)

        assertTrue(result.isCompleted)
    }

    @Test
    fun `completeTodo marks an already-completed todo as completed`() = runTest {
        val existing = TestFixtures.budgetReviewTodo().copy(isCompleted = true)
        coEvery { todoRepository.findById(existing.id!!) } returns existing
        coEvery { todoRepository.save(any()) } answers { firstArg() }

        val result = todoService.completeTodo(existing.id!!, elenaId)

        assertTrue(result.isCompleted)
    }

    @Test
    fun `completeTodo throws ForbiddenResourceException for another user's todo`() = runTest {
        val existing = TestFixtures.budgetReviewTodo()
        coEvery { todoRepository.findById(existing.id!!) } returns existing

        assertFailsWith<ForbiddenResourceException> {
            todoService.completeTodo(existing.id!!, TestFixtures.marcusChen().id!!)
        }
    }

    @Test
    fun `deleteTodo removes the document and broadcasts a DELETED event with a null entity`() = runTest {
        val existing = TestFixtures.passportRenewalTodo()
        coEvery { todoRepository.findById(existing.id!!) } returns existing
        coEvery { todoRepository.delete(existing) } returns Unit

        todoService.deleteTodo(existing.id!!, elenaId)

        coVerify(exactly = 1) { todoRepository.delete(existing) }
        val eventSlot = slot<SyncEvent>()
        verify(exactly = 1) { syncBroadcastService.broadcast(capture(eventSlot)) }
        assertEquals(SyncAction.DELETED, eventSlot.captured.action)
        assertEquals(existing.id, eventSlot.captured.entityId)
        assertEquals(null, eventSlot.captured.entity)
    }

    @Test
    fun `deleteTodo throws ForbiddenResourceException for another user's todo`() = runTest {
        val existing = TestFixtures.passportRenewalTodo()
        coEvery { todoRepository.findById(existing.id!!) } returns existing

        assertFailsWith<ForbiddenResourceException> {
            todoService.deleteTodo(existing.id!!, TestFixtures.marcusChen().id!!)
        }
        coVerify(exactly = 0) { todoRepository.delete(any()) }
    }

    @Test
    fun `reorderTodos only saves and broadcasts todos whose rank actually changed`() = runTest {
        val passport = TestFixtures.passportRenewalTodo(priorityRank = 0)
        val budget = TestFixtures.budgetReviewTodo(priorityRank = 1)
        coEvery { todoRepository.findByUserIdOrderByPriorityRankAsc(elenaId) } returns
            flowOf(passport, budget)

        val savedSlot = slot<List<TodoEntity>>()
        coEvery { todoRepository.saveAll(capture(savedSlot)) } answers {
            flowOf(*savedSlot.captured.toTypedArray())
        }

        todoService.reorderTodos(orderedIds = listOf(budget.id!!, passport.id!!), userId = elenaId)

        assertEquals(2, savedSlot.captured.size)
        assertEquals(budget.id, savedSlot.captured[0].id)
        assertEquals(0, savedSlot.captured[0].priorityRank)
        assertEquals(passport.id, savedSlot.captured[1].id)
        assertEquals(1, savedSlot.captured[1].priorityRank)
        verify(exactly = 2) { syncBroadcastService.broadcast(match { it.action == SyncAction.UPDATED }) }
    }

    @Test
    fun `reorderTodos is a no-op when the requested order matches the current order`() = runTest {
        val passport = TestFixtures.passportRenewalTodo(priorityRank = 0)
        val budget = TestFixtures.budgetReviewTodo(priorityRank = 1)
        coEvery { todoRepository.findByUserIdOrderByPriorityRankAsc(elenaId) } returns
            flowOf(passport, budget)

        todoService.reorderTodos(orderedIds = listOf(passport.id!!, budget.id!!), userId = elenaId)

        coVerify(exactly = 0) { todoRepository.saveAll(any<List<TodoEntity>>()) }
        verify(exactly = 0) { syncBroadcastService.broadcast(any()) }
    }

    @Test
    fun `getTodos maps every entity from the repository to its domain representation`() = runTest {
        val passport = TestFixtures.passportRenewalTodo()
        val budget = TestFixtures.budgetReviewTodo()
        coEvery { todoRepository.findTodos(elenaId, null, null, "work") } returns flowOf(budget)
        coEvery { todoRepository.findTodos(elenaId, null, null, null) } returns flowOf(passport, budget)

        val workOnly = todoService.getTodos(elenaId, null, null, "work").toList()
        val everything = todoService.getTodos(elenaId, null, null, null).toList()

        assertEquals(1, workOnly.size)
        assertEquals("Submit Q3 budget review to finance", workOnly[0].title)
        assertEquals(2, everything.size)
    }
}
