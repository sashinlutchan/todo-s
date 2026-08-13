package com.todo.to_do.data.remote

import com.todo.to_do.data.remote.dto.SyncEventDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SyncClient(
    private val client: HttpClient,
    private val config: ApiConfig,
    private val sessionStore: SessionStore
) {
    fun events(): Flow<SyncEventDto> = flow {
        val token = sessionStore.token ?: return@flow
        client.webSocket("${config.wsUrl}/ws/sync?token=$token") {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    emit(appJson.decodeFromString<SyncEventDto>(frame.readText()))
                }
            }
        }
    }
}

