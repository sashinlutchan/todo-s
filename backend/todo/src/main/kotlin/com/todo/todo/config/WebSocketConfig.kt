package com.todo.todo.config

import com.todo.todo.repository.websocket.SyncSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@Configuration
class WebSocketConfig(
    private val syncSocketHandler: SyncSocketHandler
) {

    @Bean
    fun webSocketHandlerMapping(): HandlerMapping {
        val map = mapOf("/ws/sync" to syncSocketHandler)
        val order = -1 // High priority mapping

        return SimpleUrlHandlerMapping(map, order)
    }

    @Bean
    fun handlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }
}
