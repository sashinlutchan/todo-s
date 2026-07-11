package com.todo.todo.config

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

private data class Sample(val id: String, val createdAt: Instant)

class JacksonConfigTest {

    private val mapper = JacksonConfig().objectMapper()

    @Test
    fun `serializes Instant as an ISO-8601 string, not a numeric timestamp`() {
        val json = mapper.writeValueAsString(Sample("1", Instant.parse("2026-07-09T00:00:00Z")))

        assertEquals("""{"id":"1","createdAt":"2026-07-09T00:00:00Z"}""", json)
    }

    @Test
    fun `round-trips an Instant field`() {
        val original = Sample("1", Instant.parse("2026-07-09T12:34:56.789Z"))

        val roundTripped = mapper.readValue(mapper.writeValueAsString(original), Sample::class.java)

        assertEquals(original, roundTripped)
    }
}
