package com.todo.todo.service

import com.todo.todo.dto.SyncEvent
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

@Service
class SyncBroadcastService {

    private val sink = Sinks.many().multicast().directBestEffort<SyncEvent>()

    fun broadcast(event: SyncEvent) {
        sink.tryEmitNext(event)
    }

    fun getEventStream(): Flux<SyncEvent> {
        return sink.asFlux()
    }
}

