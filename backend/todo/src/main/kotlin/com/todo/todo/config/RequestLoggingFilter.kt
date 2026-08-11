package com.todo.todo.config

import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * Logs every incoming HTTP request and its response status + duration.
 * Visible in Grafana under {service_name="todo-backend"}.
 *
 * Output format:
 *   --> GET /api/v1/todos
 *   <-- 200 GET /api/v1/todos (47ms)
 */
@Component
@Order(-1)
class RequestLoggingFilter : WebFilter {

    private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request  = exchange.request
        val method   = request.method.name()
        val path     = request.uri.path
        val query    = request.uri.rawQuery?.let { "?$it" } ?: ""
        val clientIp = request.remoteAddress?.address?.hostAddress ?: "unknown"

        log.info("--> {} {}{} [ip={}]", method, path, query, clientIp)

        val startMs = System.currentTimeMillis()

        return chain.filter(exchange).doFinally {
            val status   = exchange.response.statusCode?.value() ?: 0
            val duration = System.currentTimeMillis() - startMs
            val level    = if (status >= 500) "ERROR" else if (status >= 400) "WARN" else "INFO"

            when (level) {
                "ERROR" -> log.error("<-- {} {} {}{} ({}ms)", status, method, path, query, duration)
                "WARN"  -> log.warn( "<-- {} {} {}{} ({}ms)", status, method, path, query, duration)
                else    -> log.info( "<-- {} {} {}{} ({}ms)", status, method, path, query, duration)
            }
        }
    }
}
