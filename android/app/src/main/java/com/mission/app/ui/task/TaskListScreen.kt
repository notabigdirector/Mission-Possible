package com.mission.app.ui.task

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mission.app.data.entity.TaskConstants
import com.mission.app.data.entity.TaskEntity
import com.mission.app.sync.SyncStatus
import com.mission.app.ui.util.Formatters
import com.mission.app.ui.util.PriorityColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel,
    onAddTask: () -> Unit,
    onEditTask: (String) -> Unit,
    onOpenSync: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<TaskEntity?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务管理") },
                actions = {
                    SyncStatusIndicator(state.syncStatus)
                    IconButton(onClick = { viewModel.syncNow() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "立即同步")
                    }
                    IconButton(onClick = onOpenSync) {
                        Icon(Icons.Default.Settings, contentDescription = "同步设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Default.Add, contentDescription = "新增任务")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FilterChips(state, viewModel)
            ToolbarRow(state, viewModel)
            if (state.groups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "暂无任务",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "点击右下角 + 添加任务",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    items(state.groups, key = { it.project?.id ?: "none" }) { group ->
                        ProjectGroupHeader(group)
                        group.tasks.forEach { task ->
                            TaskCard(
                                task = task,
                                subtaskCount = group.subtasksByParent[task.id]?.size ?: 0,
                                now = now,
                                onToggle = { viewModel.toggle(task) },
                                onEdit = { onEditTask(task.id) },
                                onDelete = { pendingDelete = task }
                            )
                            (group.subtasksByParent[task.id].orEmpty()).forEach { sub ->
                                SubtaskRow(
                                    task = sub,
                                    now = now,
                                    onToggle = { viewModel.toggle(sub) },
                                    onEdit = { onEditTask(sub.id) },
                                    onDelete = { pendingDelete = sub }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除任务") },
            text = { Text("确定删除「${task.title}」吗？其子任务将一并删除。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { viewModel.remove(task) }
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SyncStatusIndicator(status: SyncStatus) {
    val (label, color) = when (status.state) {
        "ok" -> "已同步" to Color(0xFF2E7D32)
        "syncing" -> "同步中" to MaterialTheme.colorScheme.primary
        "error" -> "同步出错" to MaterialTheme.colorScheme.error
        "offline" -> "离线" to MaterialTheme.colorScheme.error
        else -> "未同步" to MaterialTheme.colorScheme.outline
    }
    Row(
        modifier = Modifier.padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(8.dp),
            shape = CircleShape,
            color = color
        ) {}
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun FilterChips(state: TaskListUiState, viewModel: TaskListViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = state.filter == "all",
            onClick = { viewModel.setFilter("all") },
            label = { Text("全部 ${state.tasks.size}") }
        )
        TaskConstants.STATUSES.forEach { s ->
            FilterChip(
                selected = state.filter == s,
                onClick = { viewModel.setFilter(s) },
                label = { Text("${TaskConstants.STATUS_LABELS[s]} ${state.totals[s] ?: 0}") }
            )
        }
    }
}

@Composable
private fun ToolbarRow(state: TaskListUiState, viewModel: TaskListViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val projectLabel = when (state.projectFilter) {
            "all" -> "全部项目"
            "none" -> "无项目"
            else -> state.projects.find { it.id == state.projectFilter }?.name ?: "全部项目"
        }
        FilterMenu(
            modifier = Modifier.weight(1f),
            label = projectLabel,
            options = buildList {
                add("all" to "全部项目")
                add("none" to "无项目")
                state.projects.forEach { p -> add(p.id to p.name) }
            },
            selectedKey = state.projectFilter,
            onSelect = { viewModel.setProjectFilter(it) }
        )
        Spacer(Modifier.width(8.dp))
        val sortLabels = mapOf(
            "default" to "默认",
            "name" to "按名称",
            "countdown" to "按倒计时",
            "priority" to "按优先级"
        )
        FilterMenu(
            modifier = Modifier.weight(1f),
            label = sortLabels[state.sortMode] ?: "默认",
            options = sortLabels.toList(),
            selectedKey = state.sortMode,
            onSelect = { viewModel.setSortMode(it) }
        )
        Spacer(Modifier.weight(1f))
        FilterChip(
            selected = state.hideCompleted,
            onClick = { viewModel.setHideCompleted(!state.hideCompleted) },
            label = { Text("隐藏已完成") }
        )
    }
}

@Composable
private fun FilterMenu(
    modifier: Modifier = Modifier,
    label: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = { onSelect(key); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun ProjectGroupHeader(group: TaskGroup) {
    Row(
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = group.project?.name ?: "无项目",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        if (group.project != null) {
            Text(
                text = "优先级 ${group.project.priority}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${group.tasks.size} 项",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    subtaskCount: Int,
    now: Long,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val completed = task.status == TaskConstants.COMPLETED
    val overdue = task.dueAt != null && task.dueAt <= now

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit() }
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (completed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(8.dp),
                        shape = CircleShape,
                        color = PriorityColor(task.priority)
                    ) {}
                    Spacer(Modifier.width(6.dp))
                    val meta = buildList {
                        add(TaskConstants.STATUS_LABELS[task.status] ?: task.status)
                        task.dueAt?.let { add(Formatters.countdown(it, now)) }
                        if (subtaskCount > 0) add("$subtaskCount 个子任务")
                    }.joinToString(" · ")
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (overdue) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "更多操作",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(if (completed) "标记未完成" else "标记完成") },
                        onClick = { menuExpanded = false; onToggle() }
                    )
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = { menuExpanded = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtaskRow(
    task: TaskEntity,
    now: Long,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val completed = task.status == TaskConstants.COMPLETED
    val overdue = task.dueAt != null && task.dueAt <= now

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 8.dp, top = 1.dp, bottom = 1.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit() }
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (completed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = buildList {
                    add(TaskConstants.STATUS_LABELS[task.status] ?: task.status)
                    task.dueAt?.let { add(Formatters.countdown(it, now)) }
                }.joinToString(" · ")
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (overdue) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "更多操作",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(if (completed) "标记未完成" else "标记完成") },
                        onClick = { menuExpanded = false; onToggle() }
                    )
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = { menuExpanded = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}