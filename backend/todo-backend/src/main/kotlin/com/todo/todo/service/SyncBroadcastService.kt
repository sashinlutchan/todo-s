package com.todo.todo.service

import com.todo.todo.dto.SyncEvent
import org.springframework.stereotype.Service
import reactor.core.publisher.Sinks
import reactor.core.publisher.Flux

/**
 * Fans out change events to connected devices. A single multicast sink is shared by
 * every WebSocket session; each session filters the stream down to its own userId.
 */
@Service
class SyncBroadcastService {

    private val sink: Sinks.Many<SyncEvent> =
        Sinks.many().multicast().onBackpressureBuffer()

    fun broadcast(event: SyncEvent) {
        sink.tryEmitNext(event)
    }

    fun eventsForUser(userId: String): Flux<SyncEvent> =
        sink.asFlux().filter { it.userId == userId }
}