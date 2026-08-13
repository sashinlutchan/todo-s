package com.todo.todo.support

import com.todo.todo.entity.EmailVerificationEntity
import com.todo.todo.entity.PasswordResetEntity
import com.todo.todo.entity.TodoEntity
import com.todo.todo.entity.UserEntity
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Realistic-looking sample data shared across tests, standing in for the kind of
 * records this app actually stores - not "user1"/"todo123" placeholders.
 */
object TestFixtures {

    fun elenaMartinez(
        passwordHash: String = "\$2a\$10\$placeholderHashForElenaMartinezAccount",
        displayName: String = "Elena Martinez",
        phoneNumber: String = "+15551230001",
        emailVerified: Boolean = true
    ) = UserEntity(
        id = "68a1f0c2e4b0a1a2b3c40001",
        email = "elena.martinez@protonmail.com",
        passwordHash = passwordHash,
        phoneNumber = phoneNumber,
        displayName = displayName,
        emailVerified = emailVerified,
        createdAt = Instant.parse("2026-01-15T08:30:00Z"),
        updatedAt = Instant.parse("2026-01-15T08:30:00Z")
    )

    fun marcusChen(
        passwordHash: String = "\$2a\$10\$placeholderHashForMarcusChenAccount",
        displayName: String = "Marcus Chen",
        phoneNumber: String = "+15551230002",
        emailVerified: Boolean = true
    ) = UserEntity(
        id = "68a1f0c2e4b0a1a2b3c40002",
        email = "marcus.chen@outlook.com",
        passwordHash = passwordHash,
        phoneNumber = phoneNumber,
        displayName = displayName,
        emailVerified = emailVerified,
        createdAt = Instant.parse("2026-02-01T10:00:00Z"),
        updatedAt = Instant.parse("2026-02-01T10:00:00Z")
    )

    fun passwordResetRecord(
        id: String = "68a1f0c2e4b0a1a2b3c60001",
        email: String = elenaMartinez().email,
        codeHash: String = "\$2a\$10\$placeholderHashForResetCode483920",
        used: Boolean = false,
        verified: Boolean = false,
        expiresAt: Instant = Instant.now().plus(15, ChronoUnit.MINUTES)
    ) = PasswordResetEntity(
        id = id,
        email = email,
        codeHash = codeHash,
        used = used,
        verified = verified,
        expiresAt = expiresAt,
        createdAt = Instant.now()
    )

    fun emailVerificationRecord(
        id: String = "68a1f0c2e4b0a1a2b3c70001",
        email: String = elenaMartinez().email,
        codeHash: String = "\$2a\$10\$placeholderHashForVerificationCode483920",
        expiresAt: Instant = Instant.now().plus(15, ChronoUnit.MINUTES)
    ) = EmailVerificationEntity(
        id = id,
        email = email,
        codeHash = codeHash,
        expiresAt = expiresAt,
        createdAt = Instant.now()
    )

    fun passportRenewalTodo(
        id: String = "68a1f0c2e4b0a1a2b3c50001",
        userId: String = elenaMartinez().id!!,
        priorityRank: Int = 0
    ) = TodoEntity(
        id = id,
        userId = userId,
        title = "Renew passport before the Lisbon trip",
        description = "Book an appointment at the consulate - current passport expires in November",
        isCompleted = false,
        priority = "HIGH",
        category = "personal",
        dueDate = Instant.parse("2026-08-15T09:00:00Z"),
        priorityRank = priorityRank,
        createdAt = Instant.parse("2026-07-01T12:00:00Z"),
        updatedAt = Instant.parse("2026-07-01T12:00:00Z")
    )

    fun budgetReviewTodo(
        id: String = "68a1f0c2e4b0a1a2b3c50002",
        userId: String = elenaMartinez().id!!,
        priorityRank: Int = 1
    ) = TodoEntity(
        id = id,
        userId = userId,
        title = "Submit Q3 budget review to finance",
        description = "Include the updated headcount projections from Priya",
        isCompleted = false,
        priority = "MEDIUM",
        category = "work",
        dueDate = Instant.parse("2026-07-25T17:00:00Z"),
        priorityRank = priorityRank,
        createdAt = Instant.parse("2026-07-02T09:30:00Z"),
        updatedAt = Instant.parse("2026-07-02T09:30:00Z")
    )

    fun dentistAppointmentTodo(
        id: String = "68a1f0c2e4b0a1a2b3c50003",
        userId: String = marcusChen().id!!,
        priorityRank: Int = 0
    ) = TodoEntity(
        id = id,
        userId = userId,
        title = "Book dentist appointment for Mia",
        description = null,
        isCompleted = false,
        priority = "LOW",
        category = "family",
        dueDate = Instant.now().plus(14, ChronoUnit.DAYS),
        priorityRank = priorityRank,
        createdAt = Instant.parse("2026-07-05T08:15:00Z"),
        updatedAt = Instant.parse("2026-07-05T08:15:00Z")
    )
}