package com.todo.todo.service

import com.todo.todo.dto.SyncAction
import com.todo.todo.dto.SyncEntity
import com.todo.todo.dto.SyncEvent
import reactor.test.StepVerifier
import java.time.Duration
import kotlin.test.Test

class SyncBroadcastServiceTest {

    private val service = SyncBroadcastService()

    @Test
    fun `a subscriber only receives events for their own userId`() {
        val stream = service.eventsForUser("user-1")

        StepVerifier.create(stream)
            .then {
                service.broadcast(SyncEvent("user-2", SyncEntity.TODO, SyncAction.CREATED, "todo-other"))
                service.broadcast(SyncEvent("user-1", SyncEntity.TODO, SyncAction.CREATED, "todo-mine"))
            }
            .expectNextMatches { it.entityId == "todo-mine" }
            .thenCancel()
            .verify(Duration.ofSeconds(2))
    }

    @Test
    fun `broadcasting with no subscribers does not throw`() {
        service.broadcast(SyncEvent("user-1", SyncEntity.TODO, SyncAction.DELETED, "todo-1"))
    }
}