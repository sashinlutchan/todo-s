package com.todo.to_do.data

import com.todo.to_do.data.remote.dto.AuthResponseDto
import com.todo.to_do.data.remote.dto.TodoDto
import com.todo.to_do.data.remote.dto.UserProfileDto
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

    @Test
    fun `AuthResponseDto maps token, userId, email, and displayName`() {
        val dto = AuthResponseDto(
            token = "signed.jwt.token",
            userId = "68a1f0c2e4b0a1a2b3c40001",
            email = "elena.martinez@protonmail.com",
            displayName = "Elena Martinez"
        )

        val session = dto.toDomain()

        assertEquals(dto.token, session.token)
        assertEquals(dto.userId, session.userId)
        assertEquals(dto.email, session.email)
        assertEquals("Elena Martinez", session.displayName)
    }

    @Test
    fun `UserProfileDto maps createdAt to its string form for display and persistence`() {
        val dto = UserProfileDto(
            id = "68a1f0c2e4b0a1a2b3c40001",
            email = "elena.martinez@protonmail.com",
            displayName = "Elena Martinez",
            createdAt = Instant.parse("2026-01-15T08:30:00Z")
        )

        val profile = dto.toDomain()

        assertEquals(dto.id, profile.id)
        assertEquals(dto.displayName, profile.displayName)
        assertEquals("2026-01-15T08:30:00Z", profile.createdAt)
    }
}

