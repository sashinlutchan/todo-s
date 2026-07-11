package com.todo.todo.repository.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.todo.todo.dto.SyncAction
import com.todo.todo.dto.SyncEntity
import com.todo.todo.dto.SyncEvent
import com.todo.todo.repository.security.JwtPrincipal
import com.todo.todo.repository.security.JwtService
import com.todo.todo.service.SyncBroadcastService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.reactivestreams.Publisher
import org.springframework.web.reactive.socket.HandshakeInfo
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.net.URI
import java.time.Instant
import kotlin.test.Test

class SyncSocketHandlerTest {

    private val syncBroadcastService = mockk<SyncBroadcastService>()
    private val jwtService = mockk<JwtService>()
    private val objectMapper = mockk<ObjectMapper>()
    private val handler = SyncSocketHandler(syncBroadcastService, jwtService, objectMapper)

    private fun sessionWithQuery(query: String?): WebSocketSession {
        val session = mockk<WebSocketSession>()
        val handshakeInfo = mockk<HandshakeInfo>()
        val uri = URI("ws://localhost/ws/sync" + (query?.let { "?$it" } ?: ""))
        every { handshakeInfo.uri } returns uri
        every { session.handshakeInfo } returns handshakeInfo
        every { session.close() } returns Mono.empty()
        return session
    }

    @Test
    fun `closes the session when no token query parameter is present`() {
        val session = sessionWithQuery(null)

        StepVerifier.create(handler.handle(session)).verifyComplete()

        verify { session.close() }
    }

    @Test
    fun `closes the session when the token cannot be parsed`() {
        val session = sessionWithQuery("token=bad")
        every { jwtService.parse("bad") } throws IllegalArgumentException("malformed")

        StepVerifier.create(handler.handle(session)).verifyComplete()

        verify { session.close() }
    }

    @Test
    fun `streams the authenticated user's events as text messages`() {
        val session = sessionWithQuery("token=good")
        every { jwtService.parse("good") } returns JwtPrincipal(userId = "user-1", email = "a@b.com")
        val event = SyncEvent("user-1", SyncEntity.TODO, SyncAction.CREATED, "todo-1", Instant.EPOCH)
        every { syncBroadcastService.eventsForUser("user-1") } returns Flux.just(event)
        every { objectMapper.writeValueAsString(event) } returns """{"entityId":"todo-1"}"""
        val message = mockk<WebSocketMessage>()
        every { session.textMessage("""{"entityId":"todo-1"}""") } returns message
        val outbound = slot<Publisher<WebSocketMessage>>()
        every { session.send(capture(outbound)) } returns Mono.empty()

        handler.handle(session)

        StepVerifier.create(Flux.from(outbound.captured)).expectNext(message).verifyComplete()
    }
}
