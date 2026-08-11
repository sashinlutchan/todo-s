package com.todo.to_do.data

import com.todo.to_do.data.remote.TaskFlowApi
import com.todo.to_do.data.remote.appJson
import com.todo.to_do.data.remote.dto.ForgotPasswordRequestDto
import com.todo.to_do.data.remote.dto.RegisterRequestDto
import com.todo.to_do.data.remote.dto.ResendVerificationCodeRequestDto
import com.todo.to_do.data.remote.dto.ResetPasswordRequestDto
import com.todo.to_do.data.remote.dto.VerifyEmailRequestDto
import com.todo.to_do.data.remote.dto.VerifyResetCodeRequestDto
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * These endpoints report an invalid/expired token or code as a normal 401/400 JSON body
 * (see [TaskFlowApi.verifyToken]/[TaskFlowApi.verifyResetCode]/[TaskFlowApi.resetPassword]),
 * so the important thing under test is that the body still deserializes despite the non-2xx
 * status - a plain `expectSuccess = true` client would throw and lose the `reason` field.
 */
class TaskFlowApiAuthTest {

    private fun apiRespondingWith(status: HttpStatusCode, json: String): TaskFlowApi {
        val engine = MockEngine {
            respond(
                content = json,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(appJson) }
        }
        return TaskFlowApi(client)
    }

    @Test
    fun `verifyToken deserializes a 200 valid response`() = runTest {
        val body = """
            {
              "valid": true,
              "user": {
                "id": "68a1f0c2e4b0a1a2b3c40001",
                "email": "elena.martinez@protonmail.com",
                "displayName": "Elena Martinez",
                "createdAt": "2026-01-15T08:30:00Z"
              },
              "tokenExpiresAt": "2026-07-29T10:00:00Z"
            }
        """.trimIndent()

        val response = apiRespondingWith(HttpStatusCode.OK, body).verifyToken()

        assertTrue(response.valid)
        assertEquals("Elena Martinez", response.user?.displayName)
    }

    @Test
    fun `verifyToken deserializes a 401 expired response instead of throwing`() = runTest {
        val body = """{ "valid": false, "reason": "TOKEN_EXPIRED" }"""

        val response = apiRespondingWith(HttpStatusCode.Unauthorized, body).verifyToken()

        assertFalse(response.valid)
        assertEquals("TOKEN_EXPIRED", response.reason)
    }

    @Test
    fun `verifyResetCode deserializes a 400 CODE_EXPIRED response instead of throwing`() = runTest {
        val body = """{ "valid": false, "reason": "CODE_EXPIRED" }"""

        val response = apiRespondingWith(HttpStatusCode.BadRequest, body)
            .verifyResetCode(VerifyResetCodeRequestDto(email = "elena.martinez@protonmail.com", code = "483920"))

        assertFalse(response.valid)
        assertEquals("CODE_EXPIRED", response.reason)
    }

    @Test
    fun `resetPassword deserializes a 400 RESET_TOKEN_INVALID response instead of throwing`() = runTest {
        val body = """{ "success": false, "reason": "RESET_TOKEN_INVALID" }"""

        val response = apiRespondingWith(HttpStatusCode.BadRequest, body)
            .resetPassword(ResetPasswordRequestDto(resetToken = "forged", newPassword = "NewSecureP@ss123"))

        assertFalse(response.success)
        assertEquals("RESET_TOKEN_INVALID", response.reason)
    }

    @Test
    fun `forgotPassword always deserializes a 200 message`() = runTest {
        val body = """{ "message": "If that email exists, a reset code has been sent" }"""

        val response = apiRespondingWith(HttpStatusCode.OK, body)
            .forgotPassword(ForgotPasswordRequestDto(email = "nobody.here@example.com"))

        assertEquals("If that email exists, a reset code has been sent", response.message)
    }

    @Test
    fun `register deserializes the pending-verification response`() = runTest {
        val body = """{ "message": "Account created. Enter the code we sent to verify your email.", "email": "elena.martinez@protonmail.com" }"""

        val response = apiRespondingWith(HttpStatusCode.Created, body)
            .register(RegisterRequestDto(email = "elena.martinez@protonmail.com", password = "Tr0ubad0ur&3xpanse!"))

        assertEquals("elena.martinez@protonmail.com", response.email)
        assertEquals("Account created. Enter the code we sent to verify your email.", response.message)
    }

    @Test
    fun `verifyEmail deserializes a 200 success response with a bearer token`() = runTest {
        val body = """
            {
              "success": true,
              "token": "signed.jwt.for-elena",
              "userId": "68a1f0c2e4b0a1a2b3c40001",
              "email": "elena.martinez@protonmail.com",
              "displayName": "Elena Martinez"
            }
        """.trimIndent()

        val response = apiRespondingWith(HttpStatusCode.OK, body)
            .verifyEmail(VerifyEmailRequestDto(email = "elena.martinez@protonmail.com", code = "483920"))

        assertTrue(response.success)
        assertEquals("signed.jwt.for-elena", response.token)
    }

    @Test
    fun `verifyEmail deserializes a 400 CODE_EXPIRED response instead of throwing`() = runTest {
        val body = """{ "success": false, "reason": "CODE_EXPIRED" }"""

        val response = apiRespondingWith(HttpStatusCode.BadRequest, body)
            .verifyEmail(VerifyEmailRequestDto(email = "elena.martinez@protonmail.com", code = "483920"))

        assertFalse(response.success)
        assertEquals("CODE_EXPIRED", response.reason)
    }

    @Test
    fun `resendVerificationCode always deserializes a 200 message`() = runTest {
        val body = """{ "message": "If that account needs verification, a new code has been sent" }"""

        val response = apiRespondingWith(HttpStatusCode.OK, body)
            .resendVerificationCode(ResendVerificationCodeRequestDto(email = "nobody.here@example.com"))

        assertEquals("If that account needs verification, a new code has been sent", response.message)
    }

    @Test
    fun `getProfile deserializes the authenticated user's profile`() = runTest {
        val body = """
            {
              "id": "68a1f0c2e4b0a1a2b3c40002",
              "email": "marcus.chen@outlook.com",
              "displayName": "Marcus Chen",
              "createdAt": "2026-02-01T10:00:00Z"
            }
        """.trimIndent()

        val response = apiRespondingWith(HttpStatusCode.OK, body).getProfile()

        assertEquals("marcus.chen@outlook.com", response.email)
        assertEquals("Marcus Chen", response.displayName)
    }
}
