package com.todo.to_do

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform