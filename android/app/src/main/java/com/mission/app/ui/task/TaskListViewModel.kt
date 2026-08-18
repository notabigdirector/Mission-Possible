package com.mission.app.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mission.app.AppContainer
import com.mission.app.data.entity.ProjectEntity
import com.mission.app.data.entity.TaskConstants
import com.mission.app.data.entity.TaskEntity
import com.mission.app.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskGroup(
    val project: ProjectEntity?,
    val tasks: List<TaskEntity>,
    val subtasksByParent: Map<String, List<TaskEntity>>
)

data class TaskListUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val groups: List<TaskGroup> = emptyList(),
    val filter: String = "all",
    val projectFilter: String = "all",
    val hideCompleted: Boolean = true,
    val sortMode: String = "priority",
    val syncStatus: SyncStatus = SyncStatus(),
    val totals: Map<String, Int> = emptyMap(),
    val mainTasks: List<TaskEntity> = emptyList()
)

class TaskListViewModel(container: AppContainer) : ViewModel() {

    private val taskRepo = container.taskRepository
    private val projectRepo = container.projectRepository
    private val syncManager = container.syncManager

    private val tasks = taskRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val projects = projectRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val filter = MutableStateFlow("all")
    private val projectFilter = MutableStateFlow("all")
    private val hideCompleted = MutableStateFlow(true)
    private val sortMode = MutableStateFlow("priority")

    private data class FilterState(
        val filter: String,
        val projectFilter: String,
        val hideCompleted: Boolean,
        val sortMode: String
    )

    private val filters = combine(filter, projectFilter, hideCompleted, sortMode) { f, pf, h, s ->
        FilterState(f, pf, h, s)
    }

    val uiState: StateFlow<TaskListUiState> = combine(
        tasks, projects, filters, syncManager.status
    ) { taskList, projectList, fs, sync ->
        val totals = taskList.groupingBy { it.status }.eachCount()
        TaskListUiState(
            tasks = taskList,
            projects = projectList,
            groups = buildGroups(taskList, projectList, fs.filter, fs.projectFilter, fs.hideCompleted, fs.sortMode),
            filter = fs.filter,
            projectFilter = fs.projectFilter,
            hideCompleted = fs.hideCompleted,
            sortMode = fs.sortMode,
            syncStatus = sync,
            totals = totals,
            mainTasks = taskList.filter { it.parentId == null }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState())

    fun setFilter(f: String) { filter.value = f }
    fun setProjectFilter(f: String) { projectFilter.value = f }
    fun setHideCompleted(v: Boolean) { hideCompleted.value = v }
    fun setSortMode(s: String) { sortMode.value = s }

    fun toggle(task: TaskEntity) {
        viewModelScope.launch {
            taskRepo.toggleCompleted(task.id)
            syncManager.markDirty()
        }
    }

    fun remove(task: TaskEntity) {
        viewModelScope.launch {
            taskRepo.remove(task.id)
            syncManager.markDirty()
        }
    }

    fun syncNow() {
        viewModelScope.launch { syncManager.sync() }
    }

    private fun buildGroups(
        taskList: List<TaskEntity>,
        projectList: List<ProjectEntity>,
        f: String,
        pf: String,
        hide: Boolean,
        sort: String
    ): List<TaskGroup> {
        val byParent = taskList.groupBy { it.parentId }
        val top = taskList.filter { it.parentId == null }
        val sortFn: (List<TaskEntity>) -> List<TaskEntity> = { sortTasks(it, sort) }

        // 计算每个顶级任务应显示的子任务
        fun visibleSubtasks(t: TaskEntity): List<TaskEntity> {
            val subs = byParent[t.id] ?: emptyList()
            return when {
                f != "all" -> subs.filter { it.status == f }
                hide -> subs.filter { it.status != TaskConstants.COMPLETED }
                else -> subs
            }
        }

        // 筛选顶级任务：有匹配子任务则保留；无子任务且自身状态匹配也保留
        val base = when {
            f != "all" -> top.filter { t ->
                visibleSubtasks(t).isNotEmpty() ||
                    (byParent[t.id].isNullOrEmpty() && t.status == f)
            }
            hide -> top.filter { t -> t.status != TaskConstants.COMPLETED }
            else -> top
        }
        val final = when (pf) {
            "all" -> base
            "none" -> base.filter { it.projectId == null }
            else -> base.filter { it.projectId == pf }
        }
        val grouped = final.groupBy { it.projectId }
        val groups = grouped.map { (pid, ts) ->
            val project = projectList.find { it.id == pid }
            TaskGroup(
                project = project,
                tasks = sortFn(ts),
                subtasksByParent = ts.associate { t -> t.id to sortFn(visibleSubtasks(t)) }
            )
        }
        return groups.sortedWith(
            compareBy({ it.project?.priority ?: Int.MAX_VALUE }, { it.project?.name ?: "" })
        )
    }

    private fun sortTasks(list: List<TaskEntity>, sort: String): List<TaskEntity> {
        return when (sort) {
            "name" -> list.sortedBy { it.title }
            "countdown" -> list.sortedWith(
                compareBy<TaskEntity>({ it.dueAt }, { it.createdAt })
            )
            "priority" -> list.sortedWith(
                compareBy<TaskEntity>(
                    { priorityRank(it.priority) },
                    { it.dueAt },
                    { it.createdAt }
                )
            )
            else -> list.sortedBy { it.createdAt }
        }
    }

    private fun priorityRank(p: String): Int = when (p) {
        TaskConstants.PRIORITY_HIGH -> 0
        TaskConstants.PRIORITY_MEDIUM -> 1
        else -> 2
    }
}