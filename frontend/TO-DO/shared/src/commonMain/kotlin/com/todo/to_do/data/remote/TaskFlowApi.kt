package com.todo.to_do.data.remote

import com.todo.to_do.data.remote.dto.AuthResponseDto
import com.todo.to_do.data.remote.dto.LoginRequestDto
import com.todo.to_do.data.remote.dto.RegisterRequestDto
import com.todo.to_do.data.remote.dto.ReorderRequestDto
import com.todo.to_do.data.remote.dto.TodoDto
import com.todo.to_do.data.remote.dto.TodoRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.Instant

class TaskFlowApi(private val client: HttpClient) {

    suspend fun register(request: RegisterRequestDto): AuthResponseDto =
        client.post("/api/v1/auth/register") { jsonBody(request) }.body()

    suspend fun login(request: LoginRequestDto): AuthResponseDto =
        client.post("/api/v1/auth/login") { jsonBody(request) }.body()

    suspend fun getTodos(from: Instant?, to: Instant?, category: String?): List<TodoDto> =
        client.get("/api/v1/todos") {
            from?.let { parameter("from", it.toString()) }
            to?.let { parameter("to", it.toString()) }
            category?.let { parameter("category", it) }
        }.body()

    suspend fun getTodo(id: String): TodoDto =
        client.get("/api/v1/todos/$id").body()

    suspend fun createTodo(request: TodoRequestDto): TodoDto =
        client.post("/api/v1/todos") { jsonBody(request) }.body()

    suspend fun updateTodo(id: String, request: TodoRequestDto): TodoDto =
        client.put("/api/v1/todos/$id") { jsonBody(request) }.body()

    suspend fun toggleComplete(id: String): TodoDto =
        client.patch("/api/v1/todos/$id/complete").body()

    suspend fun reorder(request: ReorderRequestDto): List<TodoDto> =
        client.patch("/api/v1/todos/reorder") { jsonBody(request) }.body()

    suspend fun deleteTodo(id: String) {
        client.delete("/api/v1/todos/$id")
    }
}

private inline fun <reified T> io.ktor.client.request.HttpRequestBuilder.jsonBody(body: T) {
    contentType(ContentType.Application.Json)
    setBody(body)
}
