package com.todo.to_do.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json

fun createHttpClient(config: ApiConfig, sessionStore: SessionStore): HttpClient = HttpClient {
    expectSuccess = true

    install(ContentNegotiation) {
        json(appJson)
    }
    install(WebSockets)
    install(Logging) {
        level = LogLevel.INFO
    }

    defaultRequest {
        url(config.baseUrl)
        sessionStore.token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }
}

