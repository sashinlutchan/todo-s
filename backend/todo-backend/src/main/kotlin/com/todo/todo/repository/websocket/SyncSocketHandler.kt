package com.todo.todo.repository.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.todo.todo.service.SyncBroadcastService
import com.todo.todo.repository.security.JwtService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

/**
 * Reactive replacement for the STOMP endpoint described in the spec: STOMP's broker model targets
 * the servlet stack and does not coexist cleanly with WebFlux, so `/ws/sync` is a plain reactive
 * WebSocket that streams JSON [com.todo.todo.dto.SyncEvent]s scoped to the caller's user.
 *
 * The client authenticates by passing its JWT as a `token` query parameter on the handshake URL.
 */
@Component
class SyncSocketHandler(
    private val syncBroadcastService: SyncBroadcastService,
    private val jwtService: JwtService,
    private val objectMapper: ObjectMapper
) : WebSocketHandler {

    override fun handle(session: WebSocketSession): Mono<Void> {
        val token = session.handshakeInfo.uri.query
            ?.split("&")
            ?.firstOrNull { it.startsWith("token=") }
            ?.substringAfter("token=")

        val userId = token?.let { runCatching { jwtService.parse(it).userId }.getOrNull() }
            ?: return session.close()

        val outbound = syncBroadcastService.eventsForUser(userId)
            .map { event -> session.textMessage(objectMapper.writeValueAsString(event)) }

        return session.send(outbound)
    }
}
