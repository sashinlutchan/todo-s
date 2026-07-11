package com.todo.to_do.data.remote

/**
 * Backend endpoints. Default host `10.0.2.2` is the Android emulator's alias for the developer
 * machine's loopback; override in DI for real devices (use the machine's LAN IP) or iOS
 * (localhost reaches the host directly there).
 */
data class ApiConfig(
    val baseUrl: String = "http://10.0.2.2:8080",
    val wsUrl: String = "ws://10.0.2.2:8080"
)
