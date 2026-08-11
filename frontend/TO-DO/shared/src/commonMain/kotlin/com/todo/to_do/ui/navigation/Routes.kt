package com.todo.to_do.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object SplashRoute

@Serializable
object AuthRoute

@Serializable
data class VerifyEmailRoute(val email: String)

@Serializable
object TodoListRoute

@Serializable
data class TodoEditorRoute(val todoId: String? = null)

@Serializable
object ForgotPasswordRoute

@Serializable
data class VerifyResetCodeRoute(val email: String)

@Serializable
data class ResetPasswordRoute(val resetToken: String)

@Serializable
object ProfileRoute
