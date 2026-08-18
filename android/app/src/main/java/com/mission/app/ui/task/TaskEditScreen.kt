package com.mission.app.ui.task

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mission.app.data.entity.TaskConstants
import com.mission.app.data.entity.TaskEntity
import com.mission.app.ui.TaskEditViewModel
import com.mission.app.ui.util.Formatters
import java.util.Calendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    viewModel: TaskEditViewModel,
    taskId: String?,
    onClose: () -> Unit
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val allMainTasks by viewModel.mainTasks.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var task by remember(taskId) { mutableStateOf<TaskEntity?>(null) }
    var loaded by remember(taskId) { mutableStateOf(taskId == null) }

    LaunchedEffect(taskId) {
        if (taskId != null) {
            task = viewModel.getTask(taskId)
            loaded = true
        }
    }

    var title by remember(taskId) { mutableStateOf("") }
    var description by remember(taskId) { mutableStateOf("") }
    var status by remember(taskId) { mutableStateOf(TaskConstants.NOT_STARTED) }
    var priority by remember(taskId) { mutableStateOf(TaskConstants.PRIORITY_MEDIUM) }
    var dueAt by remember(taskId) { mutableStateOf<Long?>(null) }
    var parentId by remember(taskId) { mutableStateOf<String?>(null) }
    var projectId by remember(taskId) { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(task, loaded) {
        val t = task
        if (loaded && t != null) {
            title = t.title
            description = t.description
            status = t.status
            priority = t.priority
            dueAt = t.dueAt
            parentId = t.parentId
            projectId = t.projectId
        }
    }

    val parentCandidates = allMainTasks.filter { it.id != taskId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (taskId == null) "新增任务" else "任务详情") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("描述") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("状态", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskConstants.STATUSES.forEach { s ->
                    FilterChip(
                        selected = status == s,
                        onClick = { status = s },
                        label = { Text(TaskConstants.STATUS_LABELS[s] ?: s) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("优先级", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    TaskConstants.PRIORITY_HIGH,
                    TaskConstants.PRIORITY_MEDIUM,
                    TaskConstants.PRIORITY_LOW
                ).forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(TaskConstants.PRIORITY_LABELS[p] ?: p) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("父任务", style = MaterialTheme.typography.titleSmall)
            ParentDropdown(
                candidates = parentCandidates,
                selectedId = parentId,
                onSelect = { id ->
                    parentId = id
                    if (id != null) {
                        val parent = parentCandidates.find { it.id == id }
                        projectId = parent?.projectId
                    }
                }
            )

            Spacer(Modifier.height(12.dp))
            Text("项目", style = MaterialTheme.typography.titleSmall)
            ProjectDropdown(
                projects = projects,
                selectedId = projectId,
                enabled = parentId == null,
                onSelect = { projectId = it }
            )

            Spacer(Modifier.height(16.dp))
            Text("截止日期", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dueAt?.let { Formatters.date(it) } ?: "未设置",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showDatePicker = true }) {
                    Text(if (dueAt == null) "选择日期" else "修改")
                }
                if (dueAt != null) {
                    TextButton(onClick = { dueAt = null }) {
                        Text("清除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        scope.launch {
                            if (taskId == null) {
                                viewModel.create(
                                    com.mission.app.data.repository.TaskInput(
                                        parentId = parentId,
                                        projectId = projectId,
                                        title = title,
                                        description = description,
                                        status = status,
                                        priority = priority,
                                        dueAt = dueAt
                                    )
                                )
                            } else {
                                viewModel.update(
                                    taskId,
                                    com.mission.app.data.repository.TaskPatch(
                                        title = title,
                                        description = description,
                                        status = status,
                                        priority = priority,
                                        dueAt = dueAt,
                                        updateDueAt = true,
                                        projectId = projectId,
                                        updateProjectId = true,
                                        parentId = parentId,
                                        updateParentId = true
                                    )
                                )
                            }
                            onClose()
                        }
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }

            val currentTask = task
            if (currentTask != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "创建于 ${Formatters.dateTime(currentTask.createdAt)} · 更新于 ${Formatters.dateTime(currentTask.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dueAt?.let {
                Calendar.getInstance().apply { timeInMillis = it }.let { cal ->
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = ms
                        cal.set(Calendar.HOUR_OF_DAY, 23)
                        cal.set(Calendar.MINUTE, 59)
                        cal.set(Calendar.SECOND, 59)
                        cal.set(Calendar.MILLISECOND, 999)
                        dueAt = cal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentDropdown(
    candidates: List<TaskEntity>,
    selectedId: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = candidates.find { it.id == selectedId }?.title ?: "主任务（无上级）"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("父任务") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("主任务（无上级）") },
                onClick = { onSelect(null); expanded = false }
            )
            candidates.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.title) },
                    onClick = { onSelect(p.id); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectDropdown(
    projects: List<com.mission.app.data.entity.ProjectEntity>,
    selectedId: String?,
    enabled: Boolean,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = projects.find { it.id == selectedId }?.name ?: "无项目"
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = if (enabled) label else "（跟随父任务）",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("项目") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("无项目") },
                onClick = { onSelect(null); expanded = false }
            )
            projects.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.name) },
                    onClick = { onSelect(p.id); expanded = false }
                )
            }
        }
    }
}