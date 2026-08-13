package com.todo.to_do.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.todo.to_do.domain.model.Todo
import com.todo.to_do.presentation.todolist.TodoListSideEffect
import com.todo.to_do.presentation.todolist.TodoListViewModel
import com.todo.to_do.presentation.todolist.TodoTab
import com.todo.to_do.ui.components.TaskFlowTopLoader
import com.todo.to_do.ui.components.ToastHost
import com.todo.to_do.ui.components.TodoRow
import com.todo.to_do.ui.components.rememberToastState
import com.todo.to_do.ui.theme.TaskFlowTheme
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun TodoListScreen(
    onOpenTodo: (String) -> Unit,
    onAddTodo: () -> Unit,
    onOpenProfile: () -> Unit = {},
    viewModel: TodoListViewModel = koinViewModel()
) {
    val state by viewModel.collectAsState()
    val colors = TaskFlowTheme.colors
    val toastState = rememberToastState()

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is TodoListSideEffect.ShowSnackbar -> toastState.show(effect.message, effect.isError)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadTodos()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTodo, containerColor = colors.accent600) {
                Text("+", color = colors.surfacePage)
            }
        },
        bottomBar = {
            TodoTabBar(selected = state.selectedTab, onSelect = viewModel::selectTab)
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.surfacePage)
            ) {
                TodoListHeader(
                    tab = state.selectedTab,
                    count = state.visibleTodos.size,
                    onOpenProfile = onOpenProfile
                )
                TaskFlowTopLoader(visible = state.isLoading)

                if (!state.isLoading && state.visibleTodos.isEmpty()) {
                    EmptyState(tab = state.selectedTab)
                } else if (state.selectedTab == TodoTab.ACTIVE) {
                    ReorderableTodoList(
                        todos = state.visibleTodos,
                        onToggle = viewModel::toggle,
                        onOpen = onOpenTodo,
                        onDelete = viewModel::delete,
                        onReorder = viewModel::reorder
                    )
                } else {
                    PlainTodoList(
                        todos = state.visibleTodos,
                        onToggle = viewModel::toggle,
                        onOpen = onOpenTodo,
                        onDelete = viewModel::delete
                    )
                }
            }
            ToastHost(toastState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun TodoListHeader(tab: TodoTab, count: Int, onOpenProfile: () -> Unit) {
    val colors = TaskFlowTheme.colors
    val subtitle = when (tab) {
        TodoTab.ACTIVE -> if (count == 1) "1 task open" else "$count tasks open"
        TodoTab.OVERDUE -> if (count == 1) "1 task overdue" else "$count tasks overdue"
        TodoTab.COMPLETED -> if (count == 1) "1 task completed" else "$count tasks completed"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Tasks",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = if (tab == TodoTab.OVERDUE && count > 0) colors.danger else colors.textSecondary
            )
        }
        TextButton(onClick = onOpenProfile) {
            Text("Profile", color = colors.accent400)
        }
    }
}

@Composable
private fun EmptyState(tab: TodoTab) {
    val colors = TaskFlowTheme.colors
    val message = when (tab) {
        TodoTab.ACTIVE -> "You're all caught up. Tap + to add a task."
        TodoTab.OVERDUE -> "Nothing overdue. Nice work!"
        TodoTab.COMPLETED -> "No completed tasks yet."
    }
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text = message, color = colors.textMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TodoTabBar(selected: TodoTab, onSelect: (TodoTab) -> Unit) {
    val colors = TaskFlowTheme.colors
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = colors.accent600,
        selectedTextColor = colors.accent600,
        unselectedIconColor = colors.textMuted,
        unselectedTextColor = colors.textMuted,
        indicatorColor = colors.accent50
    )
    NavigationBar(containerColor = colors.surfaceCard, contentColor = colors.textPrimary) {
        NavigationBarItem(
            selected = selected == TodoTab.ACTIVE,
            onClick = { onSelect(TodoTab.ACTIVE) },
            icon = { Text("≡") },
            label = { Text("Todo") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = selected == TodoTab.OVERDUE,
            onClick = { onSelect(TodoTab.OVERDUE) },
            icon = { Text("⚠") },
            label = { Text("Overdue") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = selected == TodoTab.COMPLETED,
            onClick = { onSelect(TodoTab.COMPLETED) },
            icon = { Text("✓") },
            label = { Text("Completed") },
            colors = itemColors
        )
    }
}

@Composable
private fun PlainTodoList(
    todos: List<Todo>,
    onToggle: (String) -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(todos, key = { it.id }) { todo ->
            TodoRow(
                todo = todo,
                onToggle = { onToggle(todo.id) },
                onClick = { onOpen(todo.id) },
                onDelete = { onDelete(todo.id) }
            )
        }
    }
}

@Composable
private fun ReorderableTodoList(
    todos: List<Todo>,
    onToggle: (String) -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onReorder: (List<String>) -> Unit
) {
    val colors = TaskFlowTheme.colors
    val items = remember(todos) { mutableStateListOf<Todo>().apply { addAll(todos) } }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { 76.dp.toPx() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(items, key = { _, todo -> todo.id }) { index, todo ->
            val isDragging = draggingIndex == index
            TodoRow(
                todo = todo,
                onToggle = { onToggle(todo.id) },
                onClick = { onOpen(todo.id) },
                onDelete = { onDelete(todo.id) },
                dragHandle = {
                    Text(
                        text = "≡",
                        color = colors.textMuted,
                        modifier = Modifier.pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingIndex = index
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    draggingIndex = null
                                    dragOffset = 0f
                                    onReorder(items.map { it.id })
                                },
                                onDragCancel = {
                                    draggingIndex = null
                                    dragOffset = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    val current = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                    if (dragOffset > rowHeightPx && current < items.lastIndex) {
                                        items.add(current + 1, items.removeAt(current))
                                        draggingIndex = current + 1
                                        dragOffset -= rowHeightPx
                                    } else if (dragOffset < -rowHeightPx && current > 0) {
                                        items.add(current - 1, items.removeAt(current))
                                        draggingIndex = current - 1
                                        dragOffset += rowHeightPx
                                    }
                                }
                            )
                        }
                    )
                },
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffset else 0f
                        scaleX = if (isDragging) 1.03f else 1f
                        scaleY = if (isDragging) 1.03f else 1f
                    }
                    .animateItem()
            )
        }
    }
}

