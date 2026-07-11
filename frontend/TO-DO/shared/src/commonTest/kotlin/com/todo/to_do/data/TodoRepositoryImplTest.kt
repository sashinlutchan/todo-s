package com.todo.to_do.data

import com.todo.to_do.data.remote.TaskFlowApi
import com.todo.to_do.data.remote.appJson
import com.todo.to_do.data.repository.TodoRepositoryImpl
import com.todo.to_do.domain.model.Priority
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TodoRepositoryImplTest {

    private fun repositoryReturning(json: String): TodoRepositoryImpl {
        val engine = MockEngine {
            respond(
                content = json,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(appJson) }
        }
        return TodoRepositoryImpl(TaskFlowApi(client))
    }

    @Test
    fun `getTodos deserializes and maps to domain`() = runTest {
        val body = """
            [
              {
                "id": "t1",
                "userId": "u1",
                "title": "First",
                "priority": "MEDIUM",
                "priorityRank": 0,
                "isCompleted": false,
                "createdAt": "2024-01-01T00:00:00Z",
                "updatedAt": "2024-01-01T00:00:00Z"
              }
            ]
        """.trimIndent()

        val todos = repositoryReturning(body).getTodos(null, null, null)

        assertEquals(1, todos.size)
        assertEquals("First", todos.first().title)
        assertEquals(Priority.MEDIUM, todos.first().priority)
    }
}
