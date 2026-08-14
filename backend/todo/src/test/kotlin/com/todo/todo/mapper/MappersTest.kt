package com.todo.todo.mapper

import com.todo.todo.model.Priority
import com.todo.todo.model.Todo
import com.todo.todo.model.User
import com.todo.todo.support.TestFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class MappersTest {

    @Test
    fun `TodoEntity toDomain maps every field including a HIGH priority string to the enum constant`() {
        val entity = TestFixtures.passportRenewalTodo()

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.userId, domain.userId)
        assertEquals("Renew passport before the Lisbon trip", domain.title)
        assertEquals(entity.description, domain.description)
        assertEquals(entity.isCompleted, domain.isCompleted)
        assertEquals(Priority.HIGH, domain.priority)
        assertEquals("personal", domain.category)
        assertEquals(entity.dueDate, domain.dueDate)
        assertEquals(entity.priorityRank, domain.priorityRank)
        assertEquals(entity.createdAt, domain.createdAt)
        assertEquals(entity.updatedAt, domain.updatedAt)
    }

    @Test
    fun `TodoEntity toDomain preserves a null description and a null dueDate`() {
        val entity = TestFixtures.dentistAppointmentTodo()

        val domain = entity.toDomain()

        assertNull(domain.description)
        assertEquals(Priority.LOW, domain.priority)
    }

    @Test
    fun `Todo toEntity round-trips back to an equivalent TodoEntity`() {
        val original = TestFixtures.budgetReviewTodo()

        val roundTripped = original.toDomain().toEntity()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `Todo toEntity serializes the priority enum back to its name`() {
        val todo = Todo(
            id = "68a1f0c2e4b0a1a2b3c50010",
            userId = TestFixtures.marcusChen().id!!,
            title = "Renew home insurance policy",
            description = "Compare quotes from Lemonade and State Farm before the 08/01 deadline",
            isCompleted = false,
            priority = Priority.MEDIUM,
            category = "finance",
            dueDate = Instant.parse("2026-08-01T00:00:00Z"),
            priorityRank = 3,
            createdAt = Instant.parse("2026-07-10T14:00:00Z"),
            updatedAt = Instant.parse("2026-07-10T14:00:00Z")
        )

        val entity = todo.toEntity()

        assertEquals("MEDIUM", entity.priority)
        assertEquals(todo.id, entity.id)
        assertEquals(todo.category, entity.category)
    }

    @Test
    fun `UserEntity toDomain maps id, email, passwordHash, and profile fields`() {
        val entity = TestFixtures.elenaMartinez()

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals("elena.martinez@protonmail.com", domain.email)
        assertEquals(entity.passwordHash, domain.passwordHash)
        assertEquals(entity.displayName, domain.displayName)
        assertEquals(entity.createdAt, domain.createdAt)
        assertEquals(entity.updatedAt, domain.updatedAt)
    }

    @Test
    fun `User toEntity round-trips domain-mapped fields back to UserEntity`() {
        val original = TestFixtures.marcusChen()

        val roundTripped = original.toDomain().toEntity()

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.email, roundTripped.email)
        assertEquals(original.passwordHash, roundTripped.passwordHash)
        assertEquals(original.displayName, roundTripped.displayName)
        assertEquals(original.createdAt, roundTripped.createdAt)
        assertEquals(original.updatedAt, roundTripped.updatedAt)
    }

    @Test
    fun `User toEntity preserves a null id for a not-yet-persisted user`() {
        val now = Instant.parse("2026-07-20T09:00:00Z")
        val newUser = User(
            id = null,
            email = "priya.desai@fastmail.com",
            passwordHash = "\$2a\$10\$freshlyHashedPriyaPassword",
            displayName = "Priya",
            createdAt = now,
            updatedAt = now
        )

        val entity = newUser.toEntity()

        assertNull(entity.id)
        assertEquals("priya.desai@fastmail.com", entity.email)
    }
}