package com.todo.to_do.ui.components

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun Instant.toShortLabel(): String {
    val dt = toLocalDateTime(TimeZone.currentSystemDefault())
    val month = dt.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "$month ${dt.dayOfMonth}"
}

fun Instant.toDateTimeLabel(): String {
    val dt = toLocalDateTime(TimeZone.currentSystemDefault())
    val hour12 = if (dt.hour % 12 == 0) 12 else dt.hour % 12
    val amPm = if (dt.hour < 12) "AM" else "PM"
    val minute = dt.minute.toString().padStart(2, '0')
    return "${toShortLabel()}, $hour12:$minute $amPm"
}
