package com.todo.todo.repository.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.todo.todo.dto.SyncAction
import com.todo.todo.dto.SyncEvent
import com.todo.todo.mapper.toDomain
import com.todo.todo.repository.security.JwtService
import com.todo.todo.service.SyncBroadcastService
import com.todo.todo.support.TestFixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.HandshakeInfo
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.net.URI

class SyncSocketHandlerTest {

    private val jwtService = JwtService(
        secret = "unit-test-signing-secret-must-be-at-least-32-bytes-long",
        expirationMs = 86_400_000L
    )
    private val syncBroadcastService = SyncBroadcastService()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private val handler = SyncSocketHandler(jwtService, syncBroadcastService, objectMapper)

    private fun mockSession(query: String?): WebSocketSession {
        val session = mockk<WebSocketSession>()
        val handshakeInfo = mockk<HandshakeInfo>()
        val uri = URI("wss://api.taskflow.app/ws/sync" + if (query != null) "?$query" else "")
        every { session.handshakeInfo } returns handshakeInfo
        every { handshakeInfo.uri } returns uri
        every { session.close(any()) } returns Mono.empty()
        return session
    }

    @Test
    fun `handle closes with POLICY_VIOLATION when no token query param is present`() {
        val session = mockSession(query = null)

        StepVerifier.create(handler.handle(session)).verifyComplete()

        val statusSlot = slot<CloseStatus>()
        verify(exactly = 1) { session.close(capture(statusSlot)) }
        assertEquals(CloseStatus.POLICY_VIOLATION.code, statusSlot.captured.code)
        assertEquals("Missing token", statusSlot.captured.reason)
    }

    @Test
    fun `handle closes with POLICY_VIOLATION when the token fails to parse`() {
        val session = mockSession(query = "token=not-a-real-jwt")

        StepVerifier.create(handler.handle(session)).verifyComplete()

        val statusSlot = slot<CloseStatus>()
        verify(exactly = 1) { session.close(capture(statusSlot)) }
        assertEquals("Invalid token", statusSlot.captured.reason)
    }

    @Test
    fun `handle closes with POLICY_VIOLATION when the token was signed with a different secret`() {
        val forgedToken = JwtService(
            secret = "a-completely-different-signing-secret-of-32-plus-bytes",
            expirationMs = 86_400_000L
        ).generateToken(TestFixtures.elenaMartinez().id!!, TestFixtures.elenaMartinez().email)
        val session = mockSession(query = "token=$forgedToken")

        StepVerifier.create(handler.handle(session)).verifyComplete()

        verify(exactly = 1) { session.close(match { it.reason == "Invalid token" }) }
    }

    @Test
    fun `handle streams only the authenticated user's own events as JSON text messages`() {
        val elenaId = TestFixtures.elenaMartinez().id!!
        val marcusId = TestFixtures.marcusChen().id!!
        val token = jwtService.generateToken(elenaId, TestFixtures.elenaMartinez().email)

        val session = mockSession(query = "token=$token")
        val capturedJson = mutableListOf<String>()
        every { session.textMessage(any()) } answers {
            capturedJson.add(firstArg())
            mockk()
        }
        val sentFluxSlot = slot<Flux<WebSocketMessage>>()
        every { session.send(capture(sentFluxSlot)) } returns Mono.empty()

        handler.handle(session)

        verify(exactly = 1) { session.send(any()) }

        val marcusEvent = SyncEvent(
            userId = marcusId,
            entity = TestFixtures.dentistAppointmentTodo().toDomain(),
            action = SyncAction.CREATED,
            entityId = TestFixtures.dentistAppointmentTodo().id!!
        )
        val elenaEvent = SyncEvent(
            userId = elenaId,
            entity = TestFixtures.passportRenewalTodo().toDomain(),
            action = SyncAction.CREATED,
            entityId = TestFixtures.passportRenewalTodo().id!!
        )

        StepVerifier.create(sentFluxSlot.captured.take(1))
            .then {
                syncBroadcastService.broadcast(marcusEvent)
                syncBroadcastService.broadcast(elenaEvent)
            }
            .expectNextCount(1)
            .verifyComplete()

        assertEquals(1, capturedJson.size, "only Elena's event should have been serialized into a text message")
        val delivered = objectMapper.readValue(capturedJson[0], SyncEvent::class.java)
        assertEquals(elenaId, delivered.userId)
        assertEquals("Renew passport before the Lisbon trip", delivered.entity?.title)
        assertTrue(capturedJson[0].contains("\"action\":\"CREATED\""))
    }
}