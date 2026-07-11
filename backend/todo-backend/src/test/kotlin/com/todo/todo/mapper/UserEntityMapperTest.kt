package com.todo.todo.mapper

import com.todo.todo.entity.UserEntity
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class UserEntityMapperTest {

    private val entity = UserEntity(
        id = "user-1",
        email = "a@b.com",
        passwordHash = "hashed",
        createdAt = Instant.EPOCH
    )

    @Test
    fun `toDomain maps every field`() {
        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.email, domain.email)
        assertEquals(entity.passwordHash, domain.passwordHash)
        assertEquals(entity.createdAt, domain.createdAt)
    }

    @Test
    fun `toEntity is the inverse of toDomain`() {
        val roundTripped = entity.toDomain().toEntity()

        assertEquals(entity, roundTripped)
    }
}