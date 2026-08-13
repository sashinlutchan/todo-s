package com.todo.todo.repository.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.todo.todo.repository.security.JwtService
import com.todo.todo.service.SyncBroadcastService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@Component
class SyncSocketHandler(
    private val jwtService: JwtService,
    private val syncBroadcastService: SyncBroadcastService,
    private val objectMapper: ObjectMapper
) : WebSocketHandler {

    override fun handle(session: WebSocketSession): Mono<Void> {
        val uri = session.handshakeInfo.uri
        val query = uri.query ?: ""
        val token = query.split("&")
            .map { it.split("=") }
            .firstOrNull { it.size == 2 && it[0] == "token" }
            ?.get(1)

        if (token == null) {
            return session.close(CloseStatus.POLICY_VIOLATION.withReason("Missing token"))
        }

        val principal = jwtService.parseToken(token)
        if (principal == null) {
            return session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid token"))
        }

        val userId = principal.userId

        val messageFlux = syncBroadcastService.getEventStream()
            .filter { event -> event.userId == userId }
            .map { event ->
                val json = objectMapper.writeValueAsString(event)
                session.textMessage(json)
            }

        return session.send(messageFlux)
    }
}

