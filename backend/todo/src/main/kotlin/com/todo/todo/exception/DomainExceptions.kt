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

class UserNotFoundException(userId: String) : DomainException(
    message = "User with id '$userId' not found",
    status = HttpStatus.NOT_FOUND
)

class EmailNotVerifiedException(email: String) : DomainException(
    message = "Email '$email' has not been verified",
    status = HttpStatus.FORBIDDEN
)

class MissingPhoneNumberException : DomainException(
    message = "A phone number is required to register",
    status = HttpStatus.BAD_REQUEST
)

class InvalidPhoneNumberException : DomainException(
    message = "Phone number must be in international format with a country code, e.g. +15551234567",
    status = HttpStatus.BAD_REQUEST
)
