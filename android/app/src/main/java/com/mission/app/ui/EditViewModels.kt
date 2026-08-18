package com.mission.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mission.app.AppContainer
import com.mission.app.data.entity.ProjectEntity
import com.mission.app.data.entity.TaskEntity
import com.mission.app.data.model.RegisterResponse
import com.mission.app.data.repository.ProjectInput
import com.mission.app.data.repository.TaskInput
import com.mission.app.data.repository.TaskPatch
import com.mission.app.sync.SyncConfig
import com.mission.app.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ---------- 任务编辑 ----------

class TaskEditViewModel(container: AppContainer) : ViewModel() {
    private val taskRepo = container.taskRepository
    private val projectRepo = container.projectRepository
    private val syncManager = container.syncManager

    val projects: StateFlow<List<ProjectEntity>> = projectRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val mainTasks: StateFlow<List<TaskEntity>> = taskRepo.observeAll()
        .map { it.filter { t -> t.parentId == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun getTask(id: String): TaskEntity? = taskRepo.get(id)

    fun create(input: TaskInput) {
        viewModelScope.launch {
            taskRepo.create(input)
            syncManager.markDirty()
        }
    }

    fun update(id: String, patch: TaskPatch) {
        viewModelScope.launch {
            taskRepo.update(id, patch)
            syncManager.markDirty()
        }
    }
}

// ---------- 项目管理 ----------

class ProjectViewModel(container: AppContainer) : ViewModel() {
    private val projectRepo = container.projectRepository
    private val taskRepo = container.taskRepository
    private val syncManager = container.syncManager

    val projects: StateFlow<List<ProjectEntity>> = projectRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, priority: Int) {
        viewModelScope.launch {
            projectRepo.create(ProjectInput(name, priority))
            syncManager.markDirty()
        }
    }

    fun update(id: String, name: String, priority: Int) {
        viewModelScope.launch {
            projectRepo.update(id, name, priority)
            syncManager.markDirty()
        }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            projectRepo.remove(id)
            taskRepo.setProject(id)
            syncManager.markDirty()
        }
    }
}

// ---------- 同步设置 ----------

sealed interface RegisterState {
    data object Idle : RegisterState
    data object Loading : RegisterState
    data class Success(val response: RegisterResponse) : RegisterState
    data class Error(val message: String) : RegisterState
}

class SyncSettingsViewModel(container: AppContainer) : ViewModel() {
    private val syncManager = container.syncManager

    val config: StateFlow<SyncConfig?> = container.syncPrefs.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val status: StateFlow<SyncStatus> = syncManager.status

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    fun save(config: SyncConfig) {
        viewModelScope.launch { syncManager.setConfig(config) }
    }

    fun register(serverUrl: String, name: String) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            try {
                syncManager.setConfig(
                    syncManager.currentConfig().copy(serverUrl = serverUrl.trim())
                )
                val response = syncManager.registerUser(name)
                syncManager.setConfig(
                    syncManager.currentConfig().copy(
                        token = response.token,
                        userName = response.user.name
                    )
                )
                _registerState.value = RegisterState.Success(response)
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(e.message ?: "注册失败")
            }
        }
    }

    fun importCert(bytes: ByteArray) {
        viewModelScope.launch { syncManager.importCert(bytes) }
    }

    fun syncNow() {
        viewModelScope.launch { syncManager.sync() }
    }
}