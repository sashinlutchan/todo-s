package com.todo.todo.mapper

import com.todo.todo.entity.TodoEntity
import com.todo.todo.model.Priority
import com.todo.todo.model.Todo
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class TodoEntityMapperTest {

    private val entity = TodoEntity(
        id = "todo-1",
        userId = "user-1",
        title = "Buy milk",
        description = "2%",
        dueDate = Instant.EPOCH,
        priority = Priority.HIGH.name,
        priorityRank = 3,
        category = "errands",
        isCompleted = true,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH
    )

    @Test
    fun `toDomain maps every field and parses the priority enum`() {
        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.userId, domain.userId)
        assertEquals(entity.title, domain.title)
        assertEquals(entity.description, domain.description)
        assertEquals(entity.dueDate, domain.dueDate)
        assertEquals(Priority.HIGH, domain.priority)
        assertEquals(entity.priorityRank, domain.priorityRank)
        assertEquals(entity.category, domain.category)
        assertEquals(entity.isCompleted, domain.isCompleted)
        assertEquals(entity.createdAt, domain.createdAt)
        assertEquals(entity.updatedAt, domain.updatedAt)
    }

    @Test
    fun `toEntity is the inverse of toDomain`() {
        val roundTripped = entity.toDomain().toEntity()

        assertEquals(entity, roundTripped)
    }

    @Test
    fun `toEntity serializes the priority enum back to its name`() {
        val domain = Todo(
            id = null,
            userId = "user-1",
            title = "Task",
            description = null,
            dueDate = null,
            priority = Priority.LOW,
            priorityRank = 0,
            category = null,
            isCompleted = false,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH
        )

        assertEquals("LOW", domain.toEntity().priority)
    }
}
