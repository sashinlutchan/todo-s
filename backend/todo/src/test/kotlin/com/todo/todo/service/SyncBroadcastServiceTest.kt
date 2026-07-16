package com.todo.todo.service

import com.todo.todo.dto.SyncAction
import com.todo.todo.dto.SyncEvent
import com.todo.todo.mapper.toDomain
import com.todo.todo.support.TestFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SyncBroadcastServiceTest {

    private val syncBroadcastService = SyncBroadcastService()

    private fun todoCreatedEvent() = SyncEvent(
        userId = TestFixtures.elenaMartinez().id!!,
        entity = TestFixtures.passportRenewalTodo().toDomain(),
        action = SyncAction.CREATED,
        entityId = TestFixtures.passportRenewalTodo().id!!
    )

    @Test
    fun `an event broadcast after subscribing is delivered to the subscriber`() {
        val event = todoCreatedEvent()

        StepVerifier.create(syncBroadcastService.getEventStream().take(1))
            .then { syncBroadcastService.broadcast(event) }
            .expectNext(event)
            .verifyComplete()
    }

    @Test
    fun `multiple concurrent subscribers each receive the same broadcast event`() {
        val event = todoCreatedEvent()
        val firstLatch = CountDownLatch(1)
        val secondLatch = CountDownLatch(1)
        val firstReceived = mutableListOf<SyncEvent>()
        val secondReceived = mutableListOf<SyncEvent>()

        syncBroadcastService.getEventStream().take(1).subscribe {
            firstReceived.add(it)
            firstLatch.countDown()
        }
        syncBroadcastService.getEventStream().take(1).subscribe {
            secondReceived.add(it)
            secondLatch.countDown()
        }

        syncBroadcastService.broadcast(event)

        assertTrue(firstLatch.await(1, TimeUnit.SECONDS), "first subscriber never received the event")
        assertTrue(secondLatch.await(1, TimeUnit.SECONDS), "second subscriber never received the event")
        assertEquals(listOf(event), firstReceived)
        assertEquals(listOf(event), secondReceived)
    }

    @Test
    fun `a subscriber that joins after an earlier broadcast only sees events emitted after it subscribed`() {
        syncBroadcastService.broadcast(todoCreatedEvent())

        val laterEvent = todoCreatedEvent().copy(action = SyncAction.UPDATED)

        StepVerifier.create(syncBroadcastService.getEventStream().take(1))
            .then { syncBroadcastService.broadcast(laterEvent) }
            .expectNext(laterEvent)
            .verifyComplete()
    }

    @Test
    fun `no event arrives when nothing has been broadcast`() {
        StepVerifier.create(syncBroadcastService.getEventStream())
            .expectSubscription()
            .expectNoEvent(Duration.ofMillis(200))
            .thenCancel()
            .verify()
    }
}