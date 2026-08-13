package com.todo.to_do

import android.app.Application
import com.todo.to_do.di.initKoinAndroid

class TodoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoinAndroid(this)
    }
}

