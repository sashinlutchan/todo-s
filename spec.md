# TaskFlow — KMP Todo App Spec (v2)

A cross-platform todo app: list view and alarms/reminders. Shared Compose Multiplatform UI targeting **Android + iOS only**, Spring Boot + MongoDB backend, fully self-hosted (no Firebase/cloud push), Dockerized.

---

please stick to java 25 

pleasse ensure unit test are done for frontend and backend

the project are already created 

backend project is at backend/to-do-backend 
frontend project is at frontend/TO-DO

all bacjend stuff in teh backend project folder 

and fronend stuff in the frontend folder

## 1. Tech Stack

Bro, pulled the actual current numbers instead of guessing — here's what's genuinely latest/most-popular as of mid-2026:

| Component | Version | Notes |
|---|---|---|
| Kotlin | **2.4.0** | Latest stable language release |
| Spring Boot | **4.1.0** | Runs on Spring Framework 7, min Java 25 |
| Java | **25 (LTS)** | Sane default even though 4.1 supports up to Java 26 — stick to LTS |
| MongoDB | **8.x** (`mongo:8`) | Current Docker Hub official tag line |
| Compose Multiplatform | **1.11.0** | |
| Ktor | **3.4.0** | Client + WebSocket |
| Koin | **4.2.1** + Compiler Plugin | Compile-time-safe DI, no more runtime crashes on missing bindings |
| Orbit-MVI | **10.0.0** | |
| Navigation | AndroidX Navigation (Compose Multiplatform) | Official, now stable multiplatform — see §4.6 note |

**Backend**
- Kotlin + Spring Boot 4.1.0 (Spring Framework 7)
- Spring WebFlux (reactive) + Spring Data MongoDB (Reactive)
- Spring Security + JWT
- Lombok — see note in §3.8
- Spring `@Scheduled` for alarm-sync jobs
- **Self-hosted WebSocket (STOMP)** for real-time multi-device sync — no Firebase, no APNs, no cloud push at all
- MongoDB 8.x
- Docker + Docker Compose

**Frontend (KMP — Android & iOS only)**
- Kotlin Multiplatform 2.4.0
- Compose Multiplatform 1.11.0 for shared UI
- Ktor Client 3.4.0 (networking, incl. WebSocket client)
- **Orbit-MVI 10.0.0** for state management (see §4.2)
- Koin 4.2.1 (DI, with the new Compiler Plugin for compile-time-safe wiring)
- **AndroidX Navigation (Compose Multiplatform)** — official Google-backed nav, multiplatform support went stable this year, see §4.6
- kotlinx.serialization, kotlinx.datetime, kotlinx.coroutines
- **No local database.** State lives in memory (StateFlow) for the session; every load hits the backend. Simpler surface area, no sync-conflict logic to maintain.

---

## 2. High-Level Architecture

```
┌─────────────────────────────┐
│   Compose Multiplatform UI  │  (Android / iOS)
│  Presentation Layer (MVI    │
│   via Orbit ContainerHost)  │
├─────────────────────────────┤
│      Domain Layer            │  Use cases, business models
├─────────────────────────────┤
│       Data Layer             │  Repository → Ktor (REST + WebSocket)
└──────────────┬───────────────┘
               │ REST/JSON + STOMP over WebSocket
               ▼
┌─────────────────────────────┐
│     Spring Boot Backend     │
│  Controller → Service →     │
│  Repository → MongoDB       │
├─────────────────────────────┤
│  Scheduler → WebSocket push │  (self-hosted, no cloud)
└─────────────────────────────┘
```

Alarms fire **locally on-device** using native OS scheduling (AlarmManager / UNUserNotificationCenter) — the backend never needs to "push" a notification to trigger an alert. The backend's WebSocket channel exists purely to keep alarm *data* in sync across a user's devices, so if you edit a todo's alarm time on your phone, it corrects on your tablet too.

---

## 3. Backend Spec (Spring Boot)

### 3.1 Package Structure

```
com.taskflow.backend
├── TaskFlowApplication.kt
├── config/
│   ├── SecurityConfig.kt
│   ├── MongoConfig.kt
│   ├── WebSocketConfig.kt
│   └── SchedulerConfig.kt
├── domain/
│   ├── model/          # Todo, Alarm, User, Category
│   └── exception/      # TodoNotFoundException, etc.
├── application/
│   ├── service/        # TodoService, AlarmService, AuthService, SyncBroadcastService
│   └── dto/            # Request/Response DTOs + mappers
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/     # Mongo @Document classes
│   │   ├── repository/ # ReactiveMongoRepository interfaces
│   │   └── mapper/     # Entity <-> Domain mappers
│   ├── scheduler/       # AlarmSyncJob
│   └── websocket/       # AlarmSocketHandler / STOMP controller
└── api/
    ├── controller/      # TodoController, AlarmController, AuthController
    └── advice/          # GlobalExceptionHandler
```

Same layering discipline as before: controllers speak DTOs, services speak domain models, repositories speak Mongo entities. Nobody skips a layer.

### 3.2 Core Domain Models

```kotlin
data class Todo(
    val id: String?,
    val userId: String,
    val title: String,
    val description: String?,
    val dueDate: Instant?,
    val priority: Priority,
    val category: String?,
    val isCompleted: Boolean = false,
    val alarmIds: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class Priority { LOW, MEDIUM, HIGH }

data class Alarm(
    val id: String?,
    val todoId: String,
    val userId: String,
    val triggerTime: Instant,
    val repeatRule: RepeatRule?,  // NONE, DAILY, WEEKLY, MONTHLY, CUSTOM_CRON
    val status: AlarmStatus        // SCHEDULED, ACKNOWLEDGED, CANCELLED
)
```

### 3.3 MongoDB Schema

**`todos`**
```json
{
  "_id": ObjectId,
  "userId": "string",
  "title": "string",
  "description": "string",
  "dueDate": ISODate,
  "priority": "HIGH",
  "category": "work",
  "isCompleted": false,
  "alarmIds": ["..."],
  "createdAt": ISODate,
  "updatedAt": ISODate
}
```
Index: `{ userId: 1, dueDate: 1 }` — powers list range queries.

**`alarms`**
```json
{
  "_id": ObjectId,
  "todoId": "string",
  "userId": "string",
  "triggerTime": ISODate,
  "repeatRule": "NONE",
  "status": "SCHEDULED"
}
```
Index: `{ userId: 1, status: 1 }`.

**`users`** — email, hashed password. No device tokens needed since there's no push service.

### 3.4 API Contract (v1)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/auth/register` | Create account |
| POST | `/api/v1/auth/login` | Get JWT |
| GET | `/api/v1/todos?from=&to=&category=` | List/filter todos |
| GET | `/api/v1/todos/{id}` | Get one |
| POST | `/api/v1/todos` | Create |
| PUT | `/api/v1/todos/{id}` | Update |
| PATCH | `/api/v1/todos/{id}/complete` | Toggle done |
| DELETE | `/api/v1/todos/{id}` | Delete |
| POST | `/api/v1/todos/{id}/alarms` | Attach alarm |
| DELETE | `/api/v1/alarms/{id}` | Cancel alarm |
| `WS` | `/ws/sync` | STOMP endpoint, broadcasts todo/alarm change events to the user's connected devices |

### 3.5 Alarm Flow — No Cloud, No Push Service

This is the bit that changes the most without Firebase in the picture. The design leans entirely on native OS scheduling instead of a push service:

1. Client creates/edits a todo with an alarm → `POST /api/v1/todos/{id}/alarms`.
2. Backend persists the alarm and, in the same request/response cycle, returns the alarm data to the client.
3. Client immediately schedules the alarm **locally** via the `AlarmScheduler` expect/actual (AlarmManager on Android, `UNUserNotificationCenter` on iOS). The device fires the alert itself — no server round-trip needed at alert time.
4. If the user has the app open on a second device, the backend broadcasts the change over the `/ws/sync` STOMP topic, and that device reschedules its local alarm to match.
5. A background `AlarmSyncJob` (`@Scheduled`) periodically reconciles any alarms that drifted (e.g. device was offline when the change happened), so on next app open the client re-fetches and re-schedules anything it missed.

Trade-off worth knowing: without a cloud push service, an alert fires only if the alarm was successfully scheduled locally *before* the trigger time — i.e. the user had opened the app at least once after creating/editing that alarm. That's a fair trade for "no cloud, no third-party dependency," but flag it if 100%-guaranteed delivery across devices ever becomes a hard requirement — at that point you'd need a self-hosted push relay (e.g. running your own APNs/FCM-equivalent), which is a much bigger lift.

### 3.6 Cross-Cutting Concerns
- **Validation:** `jakarta.validation` on DTOs.
- **Error handling:** one `@RestControllerAdvice`, consistent `ErrorResponse` body.
- **Security:** JWT bearer auth, `userId` pulled from `SecurityContext`, never trusted from the request body.

### 3.7 Docker

**`Dockerfile`**
```dockerfile
FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**`docker-compose.yml`**
```yaml
services:
  mongo:
    image: mongo:8
    ports: ["27017:27017"]
    volumes: ["mongo_data:/data/db"]
    environment:
      MONGO_INITDB_ROOT_USERNAME: root
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD}

  backend:
    build: .
    ports: ["8080:8080"]
    depends_on: ["mongo"]
    environment:
      SPRING_DATA_MONGODB_URI: mongodb://root:${MONGO_PASSWORD}@mongo:27017/taskflow?authSource=admin
      JWT_SECRET: ${JWT_SECRET}

volumes:
  mongo_data:
```

Everything runs on your own infra with `docker compose up` — zero external cloud dependency, which lines up with keeping Firebase out of the picture entirely.

### 3.8 On Lombok

Straight talk on this one: in a Kotlin Spring Boot codebase, Lombok mostly solves problems Kotlin doesn't have — `data class` already generates `equals`/`hashCode`/`toString`/`copy`, and constructor-based DI removes the need for `@RequiredArgsConstructor`. So Lombok won't be doing much on the Kotlin side of this codebase.

Where it earns its spot: if any part of the backend ends up being plain Java (a legacy module, a shared library, a Java-based test util), that's where `@Data`, `@Builder`, and `@Slf4j` genuinely cut boilerplate. Spec includes it as an available dependency for exactly that scenario — Gradle setup below — but the Kotlin domain/DTO classes stay idiomatic Kotlin rather than reaching for Lombok annotations that duplicate what the language already gives you for free.

```kotlin
// build.gradle.kts
dependencies {
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
}
```

---

## 4. Frontend Spec (KMP + Compose Multiplatform, Android & iOS)

### 4.1 Module Structure

```
TaskFlow/
├── shared/
│   ├── commonMain/
│   │   ├── domain/
│   │   │   ├── model/         # Todo, Alarm
│   │   │   └── usecase/       # GetTodosUseCase, CreateTodoUseCase, ScheduleAlarmUseCase
│   │   ├── data/
│   │   │   ├── remote/        # TaskFlowApi (Ktor REST + WebSocket client), DTOs
│   │   │   └── repository/    # TodoRepositoryImpl — wraps remote calls, no local persistence
│   │   ├── presentation/
│   │   │   ├── todolist/      # TodoListContainerHost (Orbit), State, SideEffect
│   │   │   └── alarm/         # AlarmScheduler expect/actual lives here
│   │   └── di/                 # Koin modules
│   ├── androidMain/            # actual AlarmScheduler (AlarmManager)
│   └── iosMain/                 # actual AlarmScheduler (UNUserNotificationCenter)
├── androidApp/                  # Compose screens, MainActivity
└── iosApp/                       # Compose UI hosted via UIViewController bridge
```

### 4.2 State Management: Orbit-MVI

Rather than hand-rolling MVI with raw `StateFlow` + a manual reducer, **Orbit-MVI** is the pick here — it's a proper Kotlin Multiplatform state-machine framework built for exactly this: unidirectional state + side effects, works identically across Android and iOS since it's pure `commonMain`, and it gives you structured `intent { }` blocks instead of a hand-rolled `when` reducer.

```kotlin
class TodoListViewModel(
    private val getTodos: GetTodosUseCase,
    private val toggleComplete: ToggleCompleteUseCase
) : ContainerHost<TodoListState, TodoListSideEffect>, ViewModel() {

    override val container = container<TodoListState, TodoListSideEffect>(TodoListState())

    fun loadTodos() = intent {
        reduce { state.copy(isLoading = true) }
        val todos = getTodos()
        reduce { state.copy(todos = todos, isLoading = false) }
    }

    fun toggle(id: String) = intent {
        toggleComplete(id)
        postSideEffect(TodoListSideEffect.ShowSnackbar("Updated"))
    }
}

data class TodoListState(
    val todos: List<Todo> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface TodoListSideEffect {
    data class ShowSnackbar(val message: String) : TodoListSideEffect
}
```

Compose UI collects `container.stateFlow` and `container.sideEffectFlow` directly — no extra glue code needed.

### 4.3 Repository Pattern — No Local DB

`TodoRepositoryImpl` talks straight to `TaskFlowApi` (Ktor). No SQLDelight, no offline cache layer:

```kotlin
class TodoRepositoryImpl(
    private val api: TaskFlowApi
) : TodoRepository {
    override suspend fun getTodos(from: LocalDate, to: LocalDate): List<Todo> =
        api.getTodos(from, to).map { it.toDomain() }

    override suspend fun createTodo(todo: Todo): Todo =
        api.createTodo(todo.toDto()).toDomain()
}
```

In-memory state (held in the Orbit container) is the only cache — good enough for a todo app where "give it a second to reload" on a cold app-open is a totally fine trade-off for not maintaining a sync layer.

### 4.5 Alarms — expect/actual

```kotlin
// commonMain
expect class AlarmScheduler {
    fun schedule(alarm: Alarm)
    fun cancel(alarmId: String)
}
```
- **androidActual:** `AlarmManager.setExactAndAllowWhileIdle` → `BroadcastReceiver` → local notification.
- **iosActual:** `UNUserNotificationCenter.addNotificationRequest`.

Called directly from the `ScheduleAlarmUseCase` right after the backend confirms the alarm was created — see §3.5 for the full flow.

### 4.6 Dependency Injection & Navigation

**DI:** Koin 4.2.1 with the new **Koin Compiler Plugin** — this is the current-best setup, not just "Koin as usual." Instead of runtime-only module wiring, you annotate classes (`@Single`, `@Factory`, `@KoinViewModel`) and the compiler plugin verifies the whole dependency graph at compile time — missing bindings and misconfigured modules become compile errors instead of a runtime crash three screens deep. Modules: `networkModule`, `repositoryModule`, `useCaseModule`, `viewModelModule`. Started once in `MainActivity`/`AppDelegate`.

**Navigation:** going with the **official AndroidX Navigation library for Compose Multiplatform** rather than Voyager. Straight talk on why: Voyager's a fine library and still works, but the official nav library is Google/JetBrains-backed, went stable for multiplatform this year, and gives you type-safe routes out of the box via `toRoute()` — it's the more "popular and current" pick for a project starting fresh in mid-2026. One thing worth knowing: iOS-side lifecycle handling and swipe-back gestures are shallower on the official lib than on more specialized options like Decompose — if the list/editor/alarm nav ends up needing a lot of nested back-stack complexity, Decompose is worth a look, but for a todo app's screen count, the official lib is the right call.

```kotlin
// commonMain
@Serializable object TodoListRoute
@Serializable data class TodoEditorRoute(val todoId: String? = null)

NavHost(navController, startDestination = TodoListRoute) {
    composable<TodoListRoute> { TodoListScreen(navController) }
    composable<TodoEditorRoute> { backStackEntry ->
        val route: TodoEditorRoute = backStackEntry.toRoute()
        TodoEditorScreen(route.todoId)
    }
}
```

### 4.7 WebSocket Sync Client
`TaskFlowApi` also opens a Ktor WebSocket connection to `/ws/sync` on login, subscribed for as long as the app is foregrounded. Incoming change events dispatch an `intent { }` on the relevant Orbit container to refresh state and reschedule any affected local alarm.

---

## 5. Suggested Build Order

1. **Backend skeleton** — domain models, Mongo entities, CRUD endpoints, Docker Compose up.
2. **Auth** — JWT register/login.
3. **Shared KMP module** — domain models, Ktor REST client, repository, Koin wiring.
4. **Todo list screen** — Orbit ContainerHost + Compose UI, wired end-to-end.
5. **Todo editor screen** — shared add/edit page with due date + alarm pickers.
6. **Alarms** — backend alarm CRUD + `AlarmSyncJob`, then client `AlarmScheduler` expect/actual.
7. **WebSocket sync** — STOMP endpoint on backend, Ktor WS client on frontend, wire into Orbit intents.

---

## 6. Design System (UI/UX)

Locking in the actual visual language here so the frontend build has something concrete to work off, not just "make it look nice."

### 6.1 Color Palette

**Light mode (default)** — white surfaces, green as the single accent color. Green only appears where it means something: selected state, active nav, completion, priority markers. Everything else stays neutral so the green actually pops instead of getting lost.

| Token | Hex | Use |
|---|---|---|
| `surface-page` | `#FFFFFF` | App background |
| `surface-card` | `#FFFFFF` | Card/list-item background |
| `surface-muted` | `#F4F6F2` | Chips, input fields, subtle fills |
| `accent-50` | `#EAF3DE` | Selected chip bg, badge bg, avatar bg |
| `accent-400` | `#639922` | Active nav icon, links, secondary accents |
| `accent-600` | `#3B6D11` | Primary buttons, FAB, filled checkboxes |
| `accent-800` | `#27500A` | Text on `accent-50` backgrounds |
| `text-primary` | `#1A1D18` | Headings, body text |
| `text-secondary` | `#6B6F66` | Timestamps, metadata |
| `text-muted` | `#9B9E96` | Placeholders, disabled |
| `border` | `#E5E8E1` | Card borders, dividers |
| `danger` | `#E24B4A` | Overdue tasks, destructive actions |
| `warning` | `#EF9F27` | Medium priority, due-soon indicators |

**Dark mode** — not an afterthought bolted on, the same green identity carried through on a near-black base so it still reads as the same app, not a different one:

| Token | Hex | Use |
|---|---|---|
| `surface-page` | `#12140F` | App background |
| `surface-card` | `#1B1E17` | Card/list-item background |
| `surface-muted` | `#22251D` | Chips, input fields |
| `accent-50` (dark equiv.) | `#1F2E12` | Selected chip bg, badge bg |
| `accent-400` | `#8FCB4E` | Active nav icon, links |
| `accent-600` | `#97C459` | Primary buttons, FAB, filled checkboxes (lighter green reads better on dark) |
| `accent-800` (dark equiv.) | `#C0DD97` | Text on dark accent backgrounds |
| `text-primary` | `#F1F3EE` | Headings, body text |
| `text-secondary` | `#9CA096` | Timestamps, metadata |
| `text-muted` | `#6B6F66` | Placeholders, disabled |
| `border` | `#2A2D24` | Card borders, dividers |
| `danger` | `#F09595` | Overdue tasks (lightened for dark contrast) |
| `warning` | `#EF9F27` | Medium priority (same — already reads fine on dark) |

Implementation-wise: define these as Compose `Color` tokens in a `TaskFlowTheme.kt`, wrapped in `isSystemInDarkTheme()` so it follows the OS setting by default, with a manual override in settings if you want one. Same token names, two value sets — screens never hardcode a hex, they reference the token, so light/dark is a theme swap, not a per-screen conditional.

### 6.2 Priority — Drag to Reorder

Priority isn't just a `HIGH`/`MEDIUM`/`LOW` enum picked from a dropdown — it's a **drag-to-reorder list**. The user's own ordering *is* the priority; position 1 in the list is the top priority for that day, full stop. Simpler mental model than three abstract tiers, and it's satisfying to use.

- Each row gets a drag handle (`ti-grip-vertical`) on the left, dimmed until touched.
- On Android: `Modifier.pointerInput` + `LazyColumn` item reordering (or the `reorderable` library — a thin, well-maintained wrapper over exactly this pattern).
- On iOS via Compose Multiplatform: same Compose code path — one of the nice wins of sharing UI, drag-to-reorder doesn't need a platform-specific reimplementation.
- Reordering updates each todo's `priorityRank: Int` field and syncs the new order to the backend in one batch call (`PATCH /api/v1/todos/reorder` with an ordered list of IDs) — not N individual requests.
- While dragging: the lifted row gets a subtle scale-up (1.03x) and elevation bump; everything else animates into its new slot with a spring-based position transition, not a hard cut.

### 6.3 Motion & Transitions

Flat design doesn't mean static. A few specific moments that should feel alive:

| Interaction | Motion |
|---|---|
| Completing a task | Checkbox fills with a quick scale+fade checkmark, row fades to 60% opacity and strikes through, then animates out of the active list after ~600ms |
| Reordering (drag) | Spring-based position animation (`animateItemPlacement()` in `LazyColumn`) — items slide into new slots, no hard jumps |
| Deleting a task | Swipe-to-reveal delete action, row collapses height to 0 on confirm rather than popping out |
| Screen transitions | Shared-element-style crossfade + slight slide (list → detail), consistent with AndroidX Navigation's default `NavHost` transition spec — customize the default `enterTransition`/`exitTransition` rather than hand-rolling |
| FAB → new task form | FAB morphs into the form sheet (scale + fade) rather than the form just appearing — feels connected to the tap that triggered it |
| Alarm chip toggle | Small bounce/spring on the bell icon when an alarm is attached, not just a plain state swap |

Keep durations short — 150–250ms for most UI transitions, up to 350ms for the FAB-to-sheet morph. Anything longer starts to feel sluggish rather than polished.

### 6.4 Typography & Spacing
- One typeface, system default (SF Pro on iOS / Roboto on Android via Compose's platform default) — don't fight the platform's native text rendering for a utility app like this.
- Type scale: 22px headings / 15px body / 13px metadata / 11px badges — matches what's in the mockups above.
- 8px spacing grid throughout (8, 12, 16, 20, 24) — keeps every screen feeling like it belongs to the same app.
- Card corner radius: 14px for list items, 20px for sheets/modals, full circle for FAB and avatars.

## 7. Notes / Trade-offs
- No cloud push (§3.5) means alert delivery is best-effort across devices — the trade you're making for staying fully self-hosted. Fine for a personal/small-scale app; worth revisiting if this ever needs guaranteed cross-device delivery at scale.
- No local DB means no offline mode — every screen load needs a network round-trip. Keeps the codebase a lot smaller since there's no cache-invalidation or sync-conflict logic to write and maintain.
- Orbit-MVI over hand-rolled MVI because it's a maintained, purpose-built KMP state machine — less boilerplate, less room to get the reducer pattern subtly wrong. (Worth knowing: FlowMVI is a newer, more feature-rich alternative if you outgrow Orbit's simplicity — but Orbit's been in production since 2019 and has the bigger track record, which is why it's the pick here.)
- Official AndroidX Navigation over Voyager because it's the Google-backed, most broadly adopted option now that multiplatform support is stable — trade-off is shallower iOS gesture/lifecycle handling than a specialist lib like Decompose, which only matters if your nav graph gets genuinely complex.
- Spring Boot 4.1 is a real jump from 3.x (built on Spring Framework 7) — if you're coming from a 3.x mental model, budget a bit of ramp-up time for the migration-era changes (Jackson 3, Hibernate 7, etc.), even on a greenfield project where you won't hit the migration pain directly.
