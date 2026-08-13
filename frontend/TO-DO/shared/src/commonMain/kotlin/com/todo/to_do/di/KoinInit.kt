package com.todo.to_do.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

expect val platformModule: Module

fun initKoin(appDeclaration: KoinApplication.() -> Unit = {}) {
    startKoin {
        appDeclaration()
        modules(
            networkModule,
            repositoryModule,
            useCaseModule,
            viewModelModule,
            platformModule
        )
    }
}

