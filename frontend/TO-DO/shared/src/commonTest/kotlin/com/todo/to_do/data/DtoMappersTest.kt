package com.todo.to_do.data

import com.todo.to_do.data.remote.dto.TodoDto
import com.todo.to_do.data.remote.dto.toDomain
import com.todo.to_do.data.remote.dto.toRequestDto
import com.todo.to_do.domain.model.Priority
import com.todo.to_do.domain.repository.TodoDraft
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class DtoMappersTest {

    @Test
    fun `TodoDto maps priority string to enum`() {
        val dto = TodoDto(
            id = "t1",
            userId = "u1",
            title = "Task",
            priority = "HIGH",
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2024-01-01T00:00:00Z")
        )
        val domain = dto.toDomain()
        assertEquals(Priority.HIGH, domain.priority)
        assertEquals("t1", domain.id)
    }

    @Test
    fun `TodoDraft maps to request dto with enum name`() {
        val request = TodoDraft(title = "Task", priority = Priority.LOW, priorityRank = 3).toRequestDto()
        assertEquals("LOW", request.priority)
        assertEquals(3, request.priorityRank)
    }
}
