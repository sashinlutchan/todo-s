package com.todo.to_do.domain.model

class EmailNotVerifiedException(val email: String) : Exception("Email not verified")

