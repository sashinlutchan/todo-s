package com.todo.todo.repository

import com.todo.todo.entity.TodoEntity
import com.todo.todo.support.TestFixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Flux
import java.time.Instant

class CustomTodoRepositoryImplTest {

    private val mongoTemplate = mockk<ReactiveMongoTemplate>()
    private val repository = CustomTodoRepositoryImpl(mongoTemplate)
    private val elenaId = TestFixtures.elenaMartinez().id!!

    @Test
    fun `findTodos with no filters only constrains by userId and sorts by priorityRank ascending`() = runTest {
        val passport = TestFixtures.passportRenewalTodo()
        val querySlot = slot<Query>()
        every { mongoTemplate.find(capture(querySlot), TodoEntity::class.java) } returns Flux.just(passport)

        val result = repository.findTodos(elenaId, from = null, to = null, category = null).toList()

        assertEquals(listOf(passport), result)
        val criteria = querySlot.captured.queryObject
        assertEquals(elenaId, criteria["userId"])
        assertNull(criteria["category"])
        assertNull(criteria["dueDate"])
        assertEquals(mapOf("priorityRank" to 1), querySlot.captured.sortObject.toMap())
    }

    @Test
    fun `findTodos with only a category filters by userId and category`() = runTest {
        val budget = TestFixtures.budgetReviewTodo()
        val querySlot = slot<Query>()
        every { mongoTemplate.find(capture(querySlot), TodoEntity::class.java) } returns Flux.just(budget)

        val result = repository.findTodos(elenaId, from = null, to = null, category = "work").toList()

        assertEquals(listOf(budget), result)
        val criteria = querySlot.captured.queryObject
        assertEquals(elenaId, criteria["userId"])
        assertEquals("work", criteria["category"])
        assertNull(criteria["dueDate"])
    }

    @Test
    fun `findTodos with only a from date filters dueDate greater-than-or-equal`() = runTest {
        val querySlot = slot<Query>()
        every { mongoTemplate.find(capture(querySlot), TodoEntity::class.java) } returns Flux.empty()
        val from = Instant.parse("2026-07-01T00:00:00Z")

        repository.findTodos(elenaId, from = from, to = null, category = null).toList()

        val dueDateCriteria = querySlot.captured.queryObject["dueDate"] as Document
        assertEquals(from, dueDateCriteria["\$gte"])
        assertFalse(dueDateCriteria.containsKey("\$lte"))
    }

    @Test
    fun `findTodos with only a to date filters dueDate less-than-or-equal`() = runTest {
        val querySlot = slot<Query>()
        every { mongoTemplate.find(capture(querySlot), TodoEntity::class.java) } returns Flux.empty()
        val to = Instant.parse("2026-07-31T23:59:59Z")

        repository.findTodos(elenaId, from = null, to = to, category = null).toList()

        val dueDateCriteria = querySlot.captured.queryObject["dueDate"] as Document
        assertEquals(to, dueDateCriteria["\$lte"])
        assertFalse(dueDateCriteria.containsKey("\$gte"))
    }

    @Test
    fun `findTodos with both from and to dates filters dueDate within the range`() = runTest {
        val querySlot = slot<Query>()
        every { mongoTemplate.find(capture(querySlot), TodoEntity::class.java) } returns Flux.empty()
        val from = Instant.parse("2026-07-01T00:00:00Z")
        val to = Instant.parse("2026-07-31T23:59:59Z")

        repository.findTodos(elenaId, from = from, to = to, category = "personal").toList()

        val criteria = querySlot.captured.queryObject
        assertEquals("personal", criteria["category"])
        val dueDateCriteria = criteria["dueDate"] as Document
        assertEquals(from, dueDateCriteria["\$gte"])
        assertEquals(to, dueDateCriteria["\$lte"])
    }
}