package com.todo.to_do.domain.model

/** Thrown by [com.todo.to_do.domain.repository.AuthRepository.login] when the account exists and the
 *  password matches, but the email hasn't been verified yet - lets the caller route to the OTP
 *  screen instead of showing a generic auth-failure message. */
class EmailNotVerifiedException(val email: String) : Exception("Email not verified")
