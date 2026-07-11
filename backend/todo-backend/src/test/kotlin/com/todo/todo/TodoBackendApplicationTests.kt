package com.todo.todo

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@Disabled("Requires a running MongoDB instance; run with docker compose up before enabling.")
@SpringBootTest
class TodoBackendApplicationTests {

    @Test
    fun contextLoads() {
    }

}
