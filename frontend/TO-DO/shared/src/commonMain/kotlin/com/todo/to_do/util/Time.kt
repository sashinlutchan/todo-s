package com.todo.to_do.util

import kotlinx.datetime.Instant
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun nowInstant(): Instant =
    Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())

