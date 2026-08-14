package com.todo.todo

import com.todo.todo.support.MockBeansTestConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(MockBeansTestConfig::class)
class TodoBackendApplicationTests {

    @Test
    fun contextLoads() {
    }
}
