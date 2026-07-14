# todo-backend

Reactive Spring Boot backend for the TO-DO app: JWT auth, todo CRUD, and a WebSocket
sync feed, backed by MongoDB. Written in Kotlin using coroutines end-to-end (no
blocking I/O anywhere on the request path).

## Stack

| Concern         | Choice                                                                |
|-----------------|-----------------------------------------------------------------------|
| Language        | Kotlin 2.3.21, JVM toolchain 24                                       |
| Framework       | Spring Boot 4.1.0 (WebFlux, reactive)                                 |
| Database        | MongoDB, via Spring Data Reactive MongoDB (`CoroutineCrudRepository`) |
| Auth            | Stateless JWT (jjwt 0.12.6), BCrypt password hashing                  |
| Realtime        | Plain reactive WebSocket at `/ws/sync` (not STOMP — see below)        |
| Docs            | springdoc-openapi (Swagger UI)                                        |
| Build           | maven Kotlin                                                         |
| Tests           | JUnit 5, MockK, kotlinx-coroutines-test, Reactor `StepVerifier`       |

## Package layout

```
com.todo.todo
├── TodoBackendApplication.kt        entry point (@SpringBootApplication, @EnableScheduling)
├── config/
│   ├── SecurityConfig.kt            SecurityWebFilterChain, JWT filter wiring, permitted paths
│   ├── MongoConfig.kt               @EnableReactiveMongoRepositories
│   ├── WebSocketConfig.kt           maps /ws/sync -> SyncSocketHandler
│   ├── JacksonConfig.kt             ObjectMapper: Kotlin module + JavaTimeModule, ISO-8601 dates
│   └── OpenApiConfig.kt             Swagger bearer-auth scheme
├── controller/
│   ├── AuthController.kt            POST /api/v1/auth/register, /login
│   └── TodoController.kt            /api/v1/todos CRUD + reorder
├── service/
│   ├── AuthService.kt                register/login -> AuthResponse (JWT)
│   ├── TodoService.kt               todo CRUD + emits SyncEvents on mutation
│   └── SyncBroadcastService.kt      multicast Sinks.Many<SyncEvent> fan-out
├── repository/
│   ├── TodoRepository.kt            CoroutineCrudRepository<TodoEntity, String> + @Query finders
│   ├── UserRepository.kt            CoroutineCrudRepository<UserEntity, String>
│   ├── security/
│   │   ├── JwtService.kt            sign/parse JWTs -> JwtPrincipal
│   │   ├── JwtAuthenticationManager.kt   ReactiveAuthenticationManager
│   │   ├── BearerTokenAuthenticationConverter.kt   pulls "Bearer <token>" off the request
│   │   └── SecurityUtils.kt         currentUserId() suspend helper
│   └── websocket/
│       └── SyncSocketHandler.kt     WebSocketHandler for /ws/sync
├── model/                           domain models (Todo, User, Priority) — plain data classes
├── entity/                          MongoDB @Document models (TodoEntity, UserEntity)
├── mapper/                          TodoEntity <-> Todo, UserEntity <-> User (extension functions)
├── dto/                             request/response DTOs + TodoDto -> TodoResponse mapper
└── exception/                       DomainException hierarchy + @RestControllerAdvice handler
```

`model` (domain) is kept separate from `entity` (persistence) and `dto` (wire format) on
purpose — each layer only knows about its own package, and `mapper`/`dto` extension
functions convert between them. This is why every layer looks like it repeats the same
fields; that repetition is intentional so persistence or API shape can change independently.

## Code style / conventions used throughout

- **Coroutines, not Reactor, in application code.** Controllers and services are
  `suspend fun`. Reactor (`Mono`/`Flux`) only appears at the two points where the
  framework requires it: `SecurityWebFilterChain`/`ReactiveAuthenticationManager`
  (Spring Security WebFlux is Reactor-native) and `SyncBroadcastService`'s
  `Sinks.Many` (WebSocket send is Reactor-native). `TodoRepository` returns
  `Flow<TodoEntity>`, collected with `kotlinx.coroutines.flow.toList()`.
- **Extension-function mappers**, not mapper classes/MapStruct: `fun TodoEntity.toDomain(): Todo`,
  `fun Todo.toEntity(): TodoEntity`, `fun Todo.toResponse(): TodoResponse`. Keeps mapping
  colocated with the type and trivially unit-testable as plain functions.
- **Constructor injection everywhere**, no field injection, no `lateinit`.
- **Sealed exception hierarchy**: all domain errors extend `sealed class DomainException`
  in `exception/DomainExceptions.kt`; `GlobalExceptionHandler` maps each subtype to an
  HTTP status once, centrally, instead of try/catch in controllers.
- **`currentUserId()`** (`repository/security/SecurityUtils.kt`) is the *only* way
  controllers/services learn who's calling — always pulled from
  `ReactiveSecurityContextHolder`, never from a request body or path variable, so a
  client can never impersonate another user's `userId`.
- **Data classes for everything** that carries state (models, entities, DTOs, events) —
  immutable, `copy()`-based updates (see `TodoService.updateTodo`).
- **No `!!` except where nullability is already guaranteed by prior logic**, e.g.
  `saved.id!!` right after a Mongo `save()` (Mongo always assigns an `_id`).
- Bean wiring uses `@Configuration` + `@Bean` factory functions rather than
  component-scanned classes for anything that isn't itself a `@Component`/`@Service`.

## API surface

All endpoints are under `/api/v1`. Bearer JWT required except where noted.

**Auth** (`AuthController`, both public)
- `POST /api/v1/auth/register` — `{ email, password }` → `201` `AuthResponse { token, userId, email }`
- `POST /api/v1/auth/login` — same request/response shape

**Todos** (`TodoController`, all require `Authorization: Bearer <token>`)
- `GET /api/v1/todos?from&to&category` — optional Instant range + category filters
- `GET /api/v1/todos/{id}`
- `POST /api/v1/todos` — `201`, body `TodoRequest`
- `PUT /api/v1/todos/{id}` — full update
- `PATCH /api/v1/todos/{id}/complete` — toggles `isCompleted`
- `PATCH /api/v1/todos/reorder` — `{ orderedIds: [...] }`, rewrites `priorityRank` by list position
- `DELETE /api/v1/todos/{id}` — `204`

Every create/update/toggle/delete/reorder call also pushes a `SyncEvent` (see below).
`GET`/list endpoints never touch another user's todos — `TodoService.getTodo` throws
`ForbiddenResourceException` if `entity.userId != userId`.

**Docs**: Swagger UI at `/swagger-ui.html`, OpenAPI JSON at `/v3/api-docs` (both public).

## Auth flow

1. `POST /register` or `/login` → `AuthService` hashes/verifies the password with
   `BCryptPasswordEncoder`, then `JwtService.generateToken(userId, email)` signs an
   HS256 JWT (subject = userId, claim `email`) valid for `app.jwt.expiration-ms`.
2. Every subsequent request carries `Authorization: Bearer <token>`.
   `BearerTokenAuthenticationConverter` extracts the raw token into an unauthenticated
   `UsernamePasswordAuthenticationToken(token, token)`.
3. `JwtAuthenticationManager.authenticate()` calls `JwtService.parse()`; on success it
   returns an authenticated token whose *name* is the userId — that's what
   `currentUserId()` later reads back out.
4. `SecurityConfig` wires this converter+manager into a single `AuthenticationWebFilter`,
   registered with `NoOpServerSecurityContextRepository` (fully stateless — nothing is
   stored server-side between requests).
5. Public paths (no token needed): `POST /auth/register`, `POST /auth/login`, `/ws/**`,
   and the Swagger/OpenAPI routes. Everything else requires authentication.

## Realtime sync (`/ws/sync`)

The spec called for STOMP, but STOMP's broker model targets Spring's servlet stack and
doesn't sit cleanly on WebFlux, so this is a **plain reactive WebSocket** instead
(`SyncSocketHandler`, mapped by `WebSocketConfig`):

- Client connects to `/ws/sync?token=<jwt>` (JWT passed as a query param since raw
  WebSocket handshakes can't carry custom headers from a browser).
- The handler parses the token; on failure the socket is closed immediately.
- `TodoService` calls `SyncBroadcastService.broadcast(SyncEvent)` after every mutation.
  `SyncBroadcastService` holds one process-wide `Sinks.Many<SyncEvent>` multicast sink;
  each open session subscribes to that same stream and filters it down to
  `event.userId == <its own userId>` — so one create/update/delete fans out to every
  other device the same user has connected, in real time, without per-user queues.
- `SyncEvent` = `{ userId, entity: TODO, action: CREATED|UPDATED|DELETED, entityId, timestamp }`,
  serialized to JSON with the shared `ObjectMapper` (Kotlin + `JavaTimeModule`, dates as
  ISO-8601 strings, not epoch timestamps).

## Persistence notes

- `TodoEntity` stores `priority` as a plain `String` (not a Mongo enum representation);
  the domain/entity mapper does `Priority.valueOf(priority)` / `priority.name`.
- `TodoEntity` has a compound index `{userId: 1, dueDate: 1}` to support the
  date-range queries in `TodoRepository`; `UserEntity.email` has a unique index, which
  is what backs the `EmailAlreadyInUseException` check at registration.
- `spring.data.mongodb.auto-index-creation=true` — indexes above are created
  automatically on startup, no manual migration step.

## Configuration

`src/main/resources/application.properties`, all overridable via env var:

| Property                    | Env var               | Default                                                    |
|------------------------------|-----------------------|-------------------------------------------------------------|
| `spring.mongodb.uri`         | `SPRING_MONGODB_URI`  | `mongodb://localhost:27017/taskflow`                        |
| `server.port`                 | `SERVER_PORT`         | `8080`                                                       |
| `app.jwt.secret`              | `JWT_SECRET`          | dev-only placeholder — **must** be overridden in production |
| `app.jwt.expiration-ms`      | `JWT_EXPIRATION_MS`   | `86400000` (24h)                                             |

## Running locally

```bash
# with a local MongoDB already running on 27017
./mvnw spring-boot:run

# or via Docker (see below), which also starts Mongo for you
docker compose up --build
```

Run the test suite:

```bash
./mvnw test
```

## Docker

### `Dockerfile`

Multi-stage build: compiles the fat jar with Maven in a JDK image, then copies just
the jar into a slim JRE image for the runtime layer.

```dockerfile
FROM eclipse-temurin:24-jdk AS build
WORKDIR /app
COPY mvnw pom.xml mvnw.cmd ./
COPY .mvn .mvn
RUN chmod +x mvnw
COPY src src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### `docker-compose.yml`

Brings up MongoDB 8 and the backend together. The backend waits for Mongo's
healthcheck before starting; both `MONGO_PASSWORD` and `JWT_SECRET` are expected to
come from your shell environment or a local `.env` file (not committed).

```yaml
services:
  mongo:
    image: mongo:8
    ports: ["27017:27017"]
    volumes: ["mongo_data:/data/db"]
    environment:
      MONGO_INITDB_ROOT_USERNAME: root
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD}
    healthcheck:
      test: ["CMD", "mongosh", "--quiet", "--eval", "db.adminCommand('ping')"]
      interval: 5s
      timeout: 5s
      retries: 10
      start_period: 10s

  backend:
    build: .
    ports: ["8080:8080"]
    depends_on:
      mongo:
        condition: service_healthy
    environment:
      SPRING_MONGODB_URI: mongodb://root:${MONGO_PASSWORD}@mongo:27017/taskflow?authSource=admin
      JWT_SECRET: ${JWT_SECRET}

volumes:
  mongo_data:
```

Usage:

```bash
# create a .env file next to docker-compose.yml first:
#   MONGO_PASSWORD=<something-strong>
#   JWT_SECRET=<at-least-256-bits-of-random-data>

docker compose up --build
```

The backend becomes reachable on `http://localhost:8080`, Mongo on `localhost:27017`
(root-authenticated with `MONGO_PASSWORD`), and data persists in the `mongo_data`
named volume across restarts.