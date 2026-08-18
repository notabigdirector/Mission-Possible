package com.mission.app.data.model

import com.mission.app.data.entity.ProjectEntity
import com.mission.app.data.entity.TaskEntity
import com.mission.app.data.entity.TombstoneEntity

// ---------- 与 src/shared/types.ts 对齐的 API 模型（camelCase） ----------

data class TaskDto(
    val id: String,
    val parentId: String? = null,
    val projectId: String? = null,
    val title: String,
    val description: String = "",
    val status: String,
    val priority: String,
    val dueAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class ProjectDto(
    val id: String,
    val name: String,
    val priority: Int,
    val createdAt: Long,
    val updatedAt: Long
)

data class TombstoneDto(
    val kind: String,
    val id: String,
    val updatedAt: Long
)

data class SyncRequest(
    val tasks: List<TaskDto> = emptyList(),
    val projects: List<ProjectDto> = emptyList(),
    val deleted: List<TombstoneDto> = emptyList()
)

data class SyncResponse(
    val tasks: List<TaskDto> = emptyList(),
    val projects: List<ProjectDto> = emptyList(),
    val tombstones: List<TombstoneDto> = emptyList()
)

data class RegisterRequest(val name: String)

data class UserInfo(val id: String, val name: String)

data class RegisterResponse(val token: String, val user: UserInfo)

data class HealthResponse(val ok: Boolean)

// ---------- 映射 ----------

fun TaskEntity.toDto(): TaskDto = TaskDto(
    id = id,
    parentId = parentId,
    projectId = projectId,
    title = title,
    description = description,
    status = status,
    priority = priority,
    dueAt = dueAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TaskDto.toEntity(): TaskEntity = TaskEntity(
    id = id,
    parentId = parentId,
    projectId = projectId,
    title = title,
    description = description,
    status = status,
    priority = priority,
    dueAt = dueAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ProjectEntity.toDto(): ProjectDto = ProjectDto(
    id = id,
    name = name,
    priority = priority,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ProjectDto.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    name = name,
    priority = priority,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TombstoneEntity.toDto(): TombstoneDto = TombstoneDto(kind = kind, id = id, updatedAt = updatedAt)

fun TombstoneDto.toEntity(): TombstoneEntity = TombstoneEntity(kind = kind, id = id, updatedAt = updatedAt)