package com.todo.to_do.presentation.alarm

import kotlinx.datetime.Instant

expect class AlarmScheduler {
    fun schedule(todoId: String, title: String, triggerAt: Instant)
    fun cancel(todoId: String)
}

