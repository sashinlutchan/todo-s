package com.todo.todo.exception

sealed class DomainException(message: String) : RuntimeException(message)

class TodoNotFoundException(id: String) : DomainException("Todo not found: $id")

class UserNotFoundException(identifier: String) : DomainException("User not found: $identifier")

class EmailAlreadyInUseException(email: String) : DomainException("Email already in use: $email")

class InvalidCredentialsException : DomainException("Invalid email or password")

class ForbiddenResourceException : DomainException("You do not have access to this resource")