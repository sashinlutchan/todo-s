package com.todo.to_do.data.remote.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SyncEventDto(
    val userId: String,
    val entity: String,
    val action: String,
    val entityId: String,
    val timestamp: Instant
)

