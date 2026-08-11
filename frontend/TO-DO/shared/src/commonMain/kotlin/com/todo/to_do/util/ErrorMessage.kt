package com.todo.to_do.util

/**
 * Converts a raw [Throwable] into a clean, user-facing error message.
 *
 * Ktor exception messages embed the full request URL, HTTP method, and status code, e.g.:
 *   "io.ktor.client.plugins.ClientRequestException: 401 Unauthorized.
 *    URL: http://10.0.2.2:8080/api/v1/auth/login. Method: POST."
 *
 * This function strips all of that so the user only ever sees a generic, safe message.
 * The [fallback] is what is shown when no specific HTTP status can be inferred.
 */
fun Throwable.toUserMessage(fallback: String = "Something went wrong. Please try again."): String {
    val msg = message ?: ""
    return when {
        // 401 / 403 — credentials / permission issue
        msg.contains("401") -> "Invalid credentials. Please check and try again."
        msg.contains("403") -> "You don't have permission to do that."

        // 404 — resource not found
        msg.contains("404") -> "The requested resource could not be found."

        // 409 — conflict (e.g. email already registered)
        msg.contains("409") -> "A conflict occurred. That may already exist."

        // 422 / 400 — validation error
        msg.contains("422") || msg.contains("400") -> "Invalid input. Please check your details."

        // 500+ — server side problem
        msg.contains("500") || msg.contains("502") || msg.contains("503")
            -> "Server error. Please try again later."

        // Network / connectivity errors
        msg.contains("UnknownHostException", ignoreCase = true)
            || msg.contains("ConnectException", ignoreCase = true)
            || msg.contains("SocketException", ignoreCase = true)
            || msg.contains("Unable to connect", ignoreCase = true)
            || msg.contains("Connection refused", ignoreCase = true)
            || msg.contains("timeout", ignoreCase = true)
            -> "Unable to reach the server. Check your connection."

        else -> fallback
    }
}
