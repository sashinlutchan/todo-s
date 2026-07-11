package com.todo.to_do

import androidx.compose.ui.window.ComposeUIViewController
import com.todo.to_do.di.initKoin

private var koinStarted = false

fun MainViewController() = ComposeUIViewController {
    if (!koinStarted) {
        initKoin()
        koinStarted = true
    }
    App()
}
