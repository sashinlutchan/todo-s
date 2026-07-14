package com.todo.todo.exception

import org.springframework.http.HttpStatus

sealed class DomainException(
    override val message: String,
    val status: HttpStatus
) : RuntimeException(message)

class EmailAlreadyInUseException(email: String) : DomainException(
    message = "Email '$email' is already in use",
    status = HttpStatus.CONFLICT
)

class InvalidCredentialsException : DomainException(
    message = "Invalid email or password",
    status = HttpStatus.UNAUTHORIZED
)

class TodoNotFoundException(id: String) : DomainException(
    message = "Todo with id '$id' not found",
    status = HttpStatus.NOT_FOUND
)

class ForbiddenResourceException : DomainException(
    message = "You do not have permission to access this resource",
    status = HttpStatus.FORBIDDEN
)

class InvalidTokenException : DomainException(
    message = "Invalid or expired token",
    status = HttpStatus.UNAUTHORIZED
)
