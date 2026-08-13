package com.todo.todo.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.todo.todo.model.Priority
import com.todo.todo.model.Todo
import java.time.Instant

data class AuthRequest(
    val email: String,
    val password: String,
    val displayName: String? = null,
    val phoneNumber: String? = null
)

data class AuthResponse(
    val token: String,
    val userId: String,
    val email: String,
    val displayName: String
)

data class UserProfileDto(
    val id: String,
    val email: String,
    val displayName: String,
    val createdAt: Instant
)

data class VerifyTokenResponse(
    val valid: Boolean,
    val user: UserProfileDto? = null,
    val tokenExpiresAt: Instant? = null,
    val reason: String? = null
)

data class ForgotPasswordRequest(
    val email: String
)

data class ForgotPasswordResponse(
    val message: String
)

data class VerifyResetCodeRequest(
    val email: String,
    val code: String
)

data class VerifyResetCodeResponse(
    val valid: Boolean,
    val resetToken: String? = null,
    val expiresAt: Instant? = null,
    val reason: String? = null
)

data class ResetPasswordRequest(
    val resetToken: String,
    val newPassword: String
)

data class ResetPasswordResponse(
    val success: Boolean,
    val message: String? = null,
    val reason: String? = null
)

data class RegisterResponse(
    val message: String,
    val email: String
)

data class VerifyEmailRequest(
    val email: String,
    val code: String
)

data class VerifyEmailResponse(
    val success: Boolean,
    val token: String? = null,
    val userId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val reason: String? = null
)

data class ResendVerificationCodeRequest(
    val email: String
)

data class ResendVerificationCodeResponse(
    val message: String
)

data class TodoRequest(
    val title: String,
    val description: String?,
    @get:JsonProperty("isCompleted") val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val category: String?,
    val dueDate: Instant?
)

data class TodoResponse(
    val id: String,
    val userId: String,
    val title: String,
    val description: String?,
    @get:JsonProperty("isCompleted") val isCompleted: Boolean,
    val priority: Priority,
    val category: String?,
    val dueDate: Instant?,
    val priorityRank: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class ReorderRequest(
    val orderedIds: List<String>
)

fun Todo.toResponse(): TodoResponse = TodoResponse(
    id = id ?: "",
    userId = userId,
    title = title,
    description = description,
    isCompleted = isCompleted,
    priority = priority,
    category = category,
    dueDate = dueDate,
    priorityRank = priorityRank,
    createdAt = createdAt,
    updatedAt = updatedAt
)

enum class SyncAction {
    CREATED, UPDATED, DELETED
}

data class SyncEvent(
    val userId: String,
    val entity: Todo?,
    val action: SyncAction,
    val entityId: String,
    val timestamp: Instant = Instant.now()
)

