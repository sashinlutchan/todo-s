package com.todo.todo.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(TodoNotFoundException::class, UserNotFoundException::class)
    fun handleNotFound(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        build(HttpStatus.NOT_FOUND, ex.message)

    @ExceptionHandler(EmailAlreadyInUseException::class)
    fun handleConflict(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        build(HttpStatus.CONFLICT, ex.message)

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleUnauthorized(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        build(HttpStatus.UNAUTHORIZED, ex.message)

    @ExceptionHandler(ForbiddenResourceException::class)
    fun handleForbidden(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        build(HttpStatus.FORBIDDEN, ex.message)

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidation(ex: WebExchangeBindException): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors.joinToString("; ") {
            "${it.field}: ${it.defaultMessage}"
        }.ifBlank { "Validation failed" }
        return build(HttpStatus.BAD_REQUEST, message)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ErrorResponse> =
        build(HttpStatus.INTERNAL_SERVER_ERROR, ex.message ?: "Unexpected error")

    private fun build(status: HttpStatus, message: String?): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(
            ErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = message ?: status.reasonPhrase
            )
        )
}