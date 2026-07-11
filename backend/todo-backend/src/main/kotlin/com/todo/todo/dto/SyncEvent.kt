package com.todo.todo.dto

import java.time.Instant

enum class SyncEntity { TODO }

enum class SyncAction { CREATED, UPDATED, DELETED }

data class SyncEvent(
    val userId: String,
    val entity: SyncEntity,
    val action: SyncAction,
    val entityId: String,
    val timestamp: Instant = Instant.now()
)