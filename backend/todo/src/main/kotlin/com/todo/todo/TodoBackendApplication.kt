package com.todo.todo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class TodoBackendApplication

fun main(args: Array<String>) {
    runApplication<TodoBackendApplication>(*args)
}
