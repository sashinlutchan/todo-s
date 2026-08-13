package com.todo.to_do.di

import android.content.Context
import org.koin.dsl.module

fun initKoinAndroid(context: Context) = initKoin {
    val resources = context.resources
    val baseUrlId = resources.getIdentifier("base_url", "string", context.packageName)
    val wsUrlId = resources.getIdentifier("ws_url", "string", context.packageName)
    val baseUrl = if (baseUrlId != 0) resources.getString(baseUrlId) else "http://10.0.2.2:8080"
    val wsUrl = if (wsUrlId != 0) resources.getString(wsUrlId) else "ws://10.0.2.2:8080"
    
    properties(mapOf("BASE_URL" to baseUrl, "WS_URL" to wsUrl))
    modules(
        module {
            single<Context> { context.applicationContext }
        }
    )
}

