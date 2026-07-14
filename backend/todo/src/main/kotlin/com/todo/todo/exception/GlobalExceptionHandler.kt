package com.todo.todo.exception

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DomainException::class)
    fun handleDomainException(ex: DomainException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(ex.status)
            .body(
                ErrorResponse(
                    message = ex.message,
                    status = ex.status.value()
                )
            )
    }

    data class ErrorResponse(
        val message: String,
        val status: Int,
        val timestamp: Instant = Instant.now()
    )
}
