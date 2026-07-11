package com.todo.to_do.data.local

import kotlinx.datetime.Instant

/**
 * Local-only record of "which todo has a reminder scheduled and for when," so the editor can
 * show it again after being reopened. This is the source of truth for reminder times; the OS
 * scheduler itself doesn't expose the trigger time for an already-scheduled alarm/notification.
 */
expect class ReminderStore {
    fun save(todoId: String, triggerAt: Instant)
    fun get(todoId: String): Instant?
    fun clear(todoId: String)
}
