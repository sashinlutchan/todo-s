package com.todo.to_do.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object SplashRoute

@Serializable
object AuthRoute

@Serializable
object TodoListRoute

@Serializable
data class TodoEditorRoute(val todoId: String? = null)
