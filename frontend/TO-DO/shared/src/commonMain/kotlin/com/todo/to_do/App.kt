package com.todo.to_do

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.todo.to_do.ui.navigation.AppNavHost
import com.todo.to_do.ui.theme.TaskFlowTheme

@Composable
fun App() {
    TaskFlowTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavHost()
        }
    }
}
