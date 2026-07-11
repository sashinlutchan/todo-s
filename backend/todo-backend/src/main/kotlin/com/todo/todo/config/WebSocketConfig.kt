package com.todo.todo.config

import com.todo.todo.repository.websocket.SyncSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@Configuration
class WebSocketConfig {

    @Bean
    fun syncHandlerMapping(syncSocketHandler: SyncSocketHandler): HandlerMapping =
        SimpleUrlHandlerMapping(mapOf("/ws/sync" to syncSocketHandler), -1)

    @Bean
    fun webSocketHandlerAdapter(): WebSocketHandlerAdapter = WebSocketHandlerAdapter()
}
