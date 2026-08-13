package com.todo.to_do.util

fun Throwable.toUserMessage(fallback: String = "Something went wrong. Please try again."): String {
    val msg = message ?: ""
    return when {
        
        msg.contains("401") -> "Invalid credentials. Please check and try again."
        msg.contains("403") -> "You don't have permission to do that."

        msg.contains("404") -> "The requested resource could not be found."

        msg.contains("409") -> "A conflict occurred. That may already exist."

        msg.contains("422") || msg.contains("400") -> "Invalid input. Please check your details."

        msg.contains("500") || msg.contains("502") || msg.contains("503")
            -> "Server error. Please try again later."

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

