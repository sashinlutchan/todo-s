package com.todo.to_do

import androidx.compose.ui.window.ComposeUIViewController
import com.todo.to_do.di.initKoin

private var koinStarted = false

fun MainViewController() = ComposeUIViewController {
    if (!koinStarted) {
        val bundle = platform.Foundation.NSBundle.mainBundle
        val baseUrl = bundle.objectForInfoDictionaryKey("BASE_URL") as? String ?: "http://localhost:8080"
        val wsUrl = bundle.objectForInfoDictionaryKey("WS_URL") as? String ?: "ws://localhost:8080"
        initKoin {
            properties(mapOf("BASE_URL" to baseUrl, "WS_URL" to wsUrl))
        }
        koinStarted = true
    }
    App()
}

