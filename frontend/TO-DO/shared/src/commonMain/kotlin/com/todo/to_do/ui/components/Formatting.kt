package com.todo.to_do.ui.components

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun Instant.toShortLabel(): String {
    val dt = toLocalDateTime(TimeZone.currentSystemDefault())
    val month = dt.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "$month ${dt.dayOfMonth}, ${dt.year}"
}

fun Instant.toDateTimeLabel(): String {
    val dt = toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = dt.hour.toString().padStart(2, '0')
    val minute = dt.minute.toString().padStart(2, '0')
    return "${toShortLabel()} $hour:$minute"
}

