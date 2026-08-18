package com.mission.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mission.app.AppContainer
import com.mission.app.ui.project.ProjectManagerScreen
import com.mission.app.ui.sync.SyncSettingsScreen
import com.mission.app.ui.task.TaskEditScreen
import com.mission.app.ui.task.TaskListScreen
import com.mission.app.ui.task.TaskListViewModel

object VmFactory {
    fun taskList(container: AppContainer) = viewModelFactory {
        initializer { TaskListViewModel(container) }
    }

    fun taskEdit(container: AppContainer) = viewModelFactory {
        initializer { TaskEditViewModel(container) }
    }

    fun projects(container: AppContainer) = viewModelFactory {
        initializer { ProjectViewModel(container) }
    }

    fun sync(container: AppContainer) = viewModelFactory {
        initializer { SyncSettingsViewModel(container) }
    }
}

@Composable
fun TaskListRoute(
    container: AppContainer,
    onAddTask: () -> Unit,
    onEditTask: (String) -> Unit,
    onOpenSync: () -> Unit
) {
    val vm: TaskListViewModel = viewModel(factory = VmFactory.taskList(container))
    TaskListScreen(
        viewModel = vm,
        onAddTask = onAddTask,
        onEditTask = onEditTask,
        onOpenSync = onOpenSync
    )
}

@Composable
fun TaskEditRoute(
    container: AppContainer,
    taskId: String?,
    onClose: () -> Unit
) {
    val vm: TaskEditViewModel = viewModel(factory = VmFactory.taskEdit(container))
    TaskEditScreen(
        viewModel = vm,
        taskId = taskId,
        onClose = onClose
    )
}

@Composable
fun ProjectsRoute(container: AppContainer) {
    val vm: ProjectViewModel = viewModel(factory = VmFactory.projects(container))
    ProjectManagerScreen(viewModel = vm)
}

@Composable
fun SyncRoute(container: AppContainer, onClose: () -> Unit) {
    val vm: SyncSettingsViewModel = viewModel(factory = VmFactory.sync(container))
    SyncSettingsScreen(viewModel = vm, onClose = onClose)
}