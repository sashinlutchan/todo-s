package com.todo.todo.controller

import com.todo.todo.dto.ReorderRequest
import com.todo.todo.dto.TodoRequest
import com.todo.todo.dto.TodoResponse
import com.todo.todo.entity.TodoEntity
import com.todo.todo.model.Priority
import com.todo.todo.repository.TodoRepository
import com.todo.todo.repository.security.JwtService
import com.todo.todo.support.MockBeansTestConfig
import com.todo.todo.support.TestFixtures
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Instant

/**
 * Full-stack test hitting the todo endpoints over real HTTP, through the actual
 * JWT security filter chain (real bearer tokens, not stubbed principals) - only
 * Mongo access is faked (see MockBeansTestConfig).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(MockBeansTestConfig::class)
class TodoControllerTest {

    @LocalServerPort
    var port: Int = 0

    private val webTestClient: WebTestClient by lazy {
        WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @Autowired
    lateinit var todoRepository: TodoRepository

    @Autowired
    lateinit var jwtService: JwtService

    private val elena = TestFixtures.elenaMartinez()
    private val marcus = TestFixtures.marcusChen()
    private lateinit var elenaToken: String
    private lateinit var marcusToken: String

    @BeforeEach
    fun setUp() {
        clearMocks(todoRepository)
        elenaToken = jwtService.generateToken(elena.id!!, elena.email)
        marcusToken = jwtService.generateToken(marcus.id!!, marcus.email)
    }

    private fun <S : WebTestClient.RequestHeadersSpec<S>> S.auth(token: String): S =
        header("Authorization", "Bearer $token")

    @Test
    fun `GET todos without an Authorization header is rejected with 401`() {
        webTestClient.get().uri("/api/v1/todos")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `GET todos with a tampered token signature is rejected with 401`() {
        webTestClient.get().uri("/api/v1/todos")
            .auth("${elenaToken}tampered")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `GET todos returns only the caller's todos mapped to the response DTO`() {
        val passport = TestFixtures.passportRenewalTodo()
        val budget = TestFixtures.budgetReviewTodo()
        coEvery { todoRepository.findTodos(elena.id!!, null, null, null) } returns flowOf(passport, budget)

        webTestClient.get().uri("/api/v1/todos")
            .auth(elenaToken)
            .exchange()
            .expectStatus().isOk
            .expectBodyList(TodoResponse::class.java)
            .hasSize(2)
    }

    @Test
    fun `GET todos filters by the category query param`() {
        val budget = TestFixtures.budgetReviewTodo()
        coEvery { todoRepository.findTodos(elena.id!!, null, null, "work") } returns flowOf(budget)

        webTestClient.get().uri("/api/v1/todos?category=work")
            .auth(elenaToken)
            .exchange()
            .expectStatus().isOk
            .expectBodyList(TodoResponse::class.java)
            .hasSize(1)
    }

    @Test
    fun `GET todos by id returns 404 when the todo does not exist`() {
        coEvery { todoRepository.findById("68a1f0c2e4b0a1a2b3c5ffff") } returns null

        webTestClient.get().uri("/api/v1/todos/68a1f0c2e4b0a1a2b3c5ffff")
            .auth(elenaToken)
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.message").isEqualTo("Todo with id '68a1f0c2e4b0a1a2b3c5ffff' not found")
    }

    @Test
    fun `GET todos by id returns 403 when the todo belongs to another user`() {
        val passport = TestFixtures.passportRenewalTodo()
        coEvery { todoRepository.findById(passport.id!!) } returns passport

        webTestClient.get().uri("/api/v1/todos/${passport.id}")
            .auth(marcusToken)
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `POST todos creates a todo owned by the caller and broadcasts nothing back over HTTP`() {
        coEvery { todoRepository.countByUserId(elena.id!!) } returns 0
        val savedSlot = slot<TodoEntity>()
        coEvery { todoRepository.save(capture(savedSlot)) } answers {
            savedSlot.captured.copy(id = "68a1f0c2e4b0a1a2b3c50005")
        }
        val request = TodoRequest(
            title = "Renew driver's license",
            description = "DMV appointment before it expires end of August",
            isCompleted = false,
            priority = Priority.MEDIUM,
            category = "personal",
            dueDate = Instant.parse("2026-08-25T09:00:00Z")
        )

        webTestClient.post().uri("/api/v1/todos")
            .auth(elenaToken)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.id").isEqualTo("68a1f0c2e4b0a1a2b3c50005")
            .jsonPath("$.userId").isEqualTo(elena.id!!)
            .jsonPath("$.title").isEqualTo("Renew driver's license")
            .jsonPath("$.priorityRank").isEqualTo(0)

        assertEquals(elena.id, savedSlot.captured.userId)
    }

    @Test
    fun `PUT todos updates fields for the owner`() {
        val passport = TestFixtures.passportRenewalTodo()
        coEvery { todoRepository.findById(passport.id!!) } returns passport
        coEvery { todoRepository.save(any()) } answers { firstArg() }
        val request = TodoRequest(
            title = "Renew passport - appointment confirmed for Aug 20",
            description = "Consulate slot booked, bring proof of address",
            isCompleted = false,
            priority = Priority.HIGH,
            category = "personal",
            dueDate = Instant.parse("2026-08-20T10:30:00Z")
        )

        webTestClient.put().uri("/api/v1/todos/${passport.id}")
            .auth(elenaToken)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.title").isEqualTo("Renew passport - appointment confirmed for Aug 20")
    }

    @Test
    fun `PUT todos rejects updates to another user's todo with 403`() {
        val passport = TestFixtures.passportRenewalTodo()
        coEvery { todoRepository.findById(passport.id!!) } returns passport
        val request = TodoRequest(
            title = "Hijacked title",
            description = null,
            category = null,
            dueDate = null
        )

        webTestClient.put().uri("/api/v1/todos/${passport.id}")
            .auth(marcusToken)
            .bodyValue(request)
            .exchange()
            .expectStatus().isForbidden

        coVerify(exactly = 0) { todoRepository.save(any()) }
    }

    @Test
    fun `PATCH complete toggles isCompleted for the owner`() {
        val budget = TestFixtures.budgetReviewTodo()
        coEvery { todoRepository.findById(budget.id!!) } returns budget
        coEvery { todoRepository.save(any()) } answers { firstArg() }

        webTestClient.patch().uri("/api/v1/todos/${budget.id}/complete")
            .auth(elenaToken)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.isCompleted").isEqualTo(true)
    }

    @Test
    fun `PATCH complete rejects toggling another user's todo with 403`() {
        val budget = TestFixtures.budgetReviewTodo()
        coEvery { todoRepository.findById(budget.id!!) } returns budget

        webTestClient.patch().uri("/api/v1/todos/${budget.id}/complete")
            .auth(marcusToken)
            .exchange()
            .expectStatus().isForbidden

        coVerify(exactly = 0) { todoRepository.save(any()) }
    }

    @Test
    fun `PATCH reorder returns 204 and persists the new priority ranks in the requested order`() {
        val passport = TestFixtures.passportRenewalTodo(priorityRank = 0)
        val budget = TestFixtures.budgetReviewTodo(priorityRank = 1)
        coEvery { todoRepository.findByUserIdOrderByPriorityRankAsc(elena.id!!) } returns flowOf(passport, budget)
        val savedSlot = slot<List<TodoEntity>>()
        coEvery { todoRepository.saveAll(capture(savedSlot)) } answers {
            flowOf(*savedSlot.captured.toTypedArray())
        }

        webTestClient.patch().uri("/api/v1/todos/reorder")
            .auth(elenaToken)
            .bodyValue(ReorderRequest(orderedIds = listOf(budget.id!!, passport.id!!)))
            .exchange()
            .expectStatus().isNoContent

        assertEquals(budget.id, savedSlot.captured[0].id)
        assertEquals(0, savedSlot.captured[0].priorityRank)
        assertEquals(passport.id, savedSlot.captured[1].id)
        assertEquals(1, savedSlot.captured[1].priorityRank)
    }

    @Test
    fun `DELETE todos removes the caller's todo and returns 204`() {
        val passport = TestFixtures.passportRenewalTodo()
        coEvery { todoRepository.findById(passport.id!!) } returns passport
        coEvery { todoRepository.delete(passport) } returns Unit

        webTestClient.delete().uri("/api/v1/todos/${passport.id}")
            .auth(elenaToken)
            .exchange()
            .expectStatus().isNoContent

        coVerify(exactly = 1) { todoRepository.delete(passport) }
    }

    @Test
    fun `DELETE todos rejects deleting another user's todo with 403`() {
        val passport = TestFixtures.passportRenewalTodo()
        coEvery { todoRepository.findById(passport.id!!) } returns passport

        webTestClient.delete().uri("/api/v1/todos/${passport.id}")
            .auth(marcusToken)
            .exchange()
            .expectStatus().isForbidden

        coVerify(exactly = 0) { todoRepository.delete(any()) }
    }
}