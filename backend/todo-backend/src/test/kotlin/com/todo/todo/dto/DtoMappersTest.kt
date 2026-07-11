package com.todo.todo.dto

import com.todo.todo.model.Priority
import com.todo.todo.model.Todo
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DtoMappersTest {

    private val todo = Todo(
        id = "todo-1",
        userId = "user-1",
        title = "Buy milk",
        description = "2%",
        dueDate = Instant.EPOCH,
        priority = Priority.HIGH,
        priorityRank = 2,
        category = "errands",
        isCompleted = false,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH
    )

    @Test
    fun `Todo toResponse maps every field`() {
        val response = todo.toResponse()

        assertEquals(todo.id, response.id)
        assertEquals(todo.userId, response.userId)
        assertEquals(todo.title, response.title)
        assertEquals(todo.description, response.description)
        assertEquals(todo.dueDate, response.dueDate)
        assertEquals(todo.priority, response.priority)
        assertEquals(todo.priorityRank, response.priorityRank)
        assertEquals(todo.category, response.category)
        assertEquals(todo.isCompleted, response.isCompleted)
        assertEquals(todo.createdAt, response.createdAt)
        assertEquals(todo.updatedAt, response.updatedAt)
    }

    @Test
    fun `Todo toResponse fails fast when id is not yet assigned`() {
        assertFailsWith<NullPointerException> { todo.copy(id = null).toResponse() }
    }
}
