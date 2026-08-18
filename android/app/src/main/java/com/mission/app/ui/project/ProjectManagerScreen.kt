package com.mission.app.ui.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mission.app.data.entity.ProjectEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectManagerScreen(viewModel: com.mission.app.ui.ProjectViewModel) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<ProjectEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("项目管理") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "新增项目")
            }
        }
    ) { padding ->
        if (projects.isEmpty()) {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("暂无项目", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(project.name, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "优先级 ${project.priority}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                editing = project
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑")
                            }
                            IconButton(onClick = { pendingDelete = project }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        ProjectDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { name, priority ->
                val target = editing
                if (target == null) {
                    viewModel.create(name, priority)
                } else {
                    viewModel.update(target.id, name, priority)
                }
                showDialog = false
            }
        )
    }

    pendingDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除项目") },
            text = { Text("确定删除项目「${project.name}」吗？项目内任务将保留但移出该项目。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.remove(project.id)
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
private fun ProjectDialog(
    initial: ProjectEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var priority by remember(initial) { mutableIntStateOf(initial?.priority ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增项目" else "编辑项目") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("项目名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = priority.toString(),
                    onValueChange = { input ->
                        priority = input.toIntOrNull() ?: 0
                    },
                    label = { Text("优先级（数字越小越靠前）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, priority) },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}