package com.todo.to_do.presentation.alarm

import kotlinx.datetime.Instant

/**
 * Schedules a device-local reminder for a todo. Nothing here talks to the backend — the OS is
 * the only thing keeping track of a pending reminder between the call and it firing.
 */
expect class AlarmScheduler {
    fun schedule(todoId: String, title: String, triggerAt: Instant)
    fun cancel(todoId: String)
}
