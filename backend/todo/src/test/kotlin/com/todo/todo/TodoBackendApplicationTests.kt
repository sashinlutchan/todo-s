package com.todo.todo

import com.todo.todo.repository.TodoRepository
import com.todo.todo.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@ActiveProfiles("test")
class TodoBackendApplicationTests {

    @MockitoBean
    lateinit var reactiveMongoTemplate: ReactiveMongoTemplate

    @MockitoBean
    lateinit var todoRepository: TodoRepository

    @MockitoBean
    lateinit var userRepository: UserRepository

    @Test
    fun contextLoads() {
    }
}
