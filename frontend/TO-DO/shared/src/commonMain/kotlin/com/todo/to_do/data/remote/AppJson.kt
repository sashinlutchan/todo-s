package com.todo.to_do.data.remote

import kotlinx.serialization.json.Json

val appJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

