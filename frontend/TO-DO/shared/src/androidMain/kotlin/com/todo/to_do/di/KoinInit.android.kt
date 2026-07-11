package com.todo.to_do.di

import android.content.Context
import org.koin.dsl.module

fun initKoinAndroid(context: Context) = initKoin {
    modules(
        module {
            single<Context> { context.applicationContext }
        }
    )
}
