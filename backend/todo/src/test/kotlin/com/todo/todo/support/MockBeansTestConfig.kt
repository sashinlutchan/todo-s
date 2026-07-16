package com.todo.todo.support

import com.todo.todo.repository.TodoRepository
import com.todo.todo.repository.UserRepository
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * Replaces the real MongoDB-backed repositories with MockK fakes for `@SpringBootTest`
 * integration tests, so the full controller -> security -> service -> serialization
 * stack can be exercised over real HTTP without a live MongoDB instance. The real
 * ReactiveMongoTemplate is left auto-configured - building it (and the repository
 * proxies it backs) needs no live connection, only issuing queries would.
 */
@TestConfiguration
class MockBeansTestConfig {

    @Bean
    @Primary
    fun mockUserRepository(): UserRepository = mockk(relaxed = true)

    @Bean
    @Primary
    fun mockTodoRepository(): TodoRepository = mockk(relaxed = true)
}