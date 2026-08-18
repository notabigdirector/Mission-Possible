package com.mission.app.data.repository

import com.mission.app.data.dao.TaskDao
import com.mission.app.data.dao.TombstoneDao
import com.mission.app.data.entity.TaskConstants
import com.mission.app.data.entity.TaskEntity
import com.mission.app.data.entity.TombstoneEntity
import com.mission.app.data.model.TaskDto
import com.mission.app.data.model.TombstoneDto
import com.mission.app.data.model.toEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

private val STATUS_ORDER = mapOf(
    TaskConstants.NOT_STARTED to 0,
    TaskConstants.IN_PROGRESS to 1,
    TaskConstants.TESTING to 2,
    TaskConstants.COMPLETED to 3
)

data class TaskInput(
    val parentId: String?,
    val projectId: String?,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val dueAt: Long?
)

data class TaskPatch(
    val status: String? = null,
    val priority: String? = null,
    val title: String? = null,
    val description: String? = null,
    val dueAt: Long? = null,
    val updateDueAt: Boolean = false,
    val projectId: String? = null,
    val updateProjectId: Boolean = false,
    val parentId: String? = null,
    val updateParentId: Boolean = false
)

/**
 * 对齐 src/main/task-store.ts 的本地存储语义：
 * 子任务创建继承父任务 projectId、删除递归写墓碑、同步后清理孤儿任务。
 */
class TaskRepository(
    private val taskDao: TaskDao,
    private val tombstoneDao: TombstoneDao
) {
    fun observeAll(): Flow<List<TaskEntity>> = taskDao.observeAll()

    suspend fun all(): List<TaskEntity> = taskDao.all()

    suspend fun get(id: String): TaskEntity? = taskDao.get(id)

    suspend fun tombstones(): List<TombstoneEntity> = tombstoneDao.all().filter { it.kind == "task" }

    suspend fun listSuspend(): List<TaskEntity> =
        taskDao.all().sortedWith(
            compareBy({ STATUS_ORDER[it.status] ?: 0 }, { -it.createdAt })
        )

    suspend fun create(input: TaskInput): TaskEntity {
        val now = System.currentTimeMillis()
        var projectId = input.projectId
        if (input.parentId != null) {
            val parent = taskDao.get(input.parentId) ?: throw IllegalStateException("无效的父任务")
            if (parent.parentId != null) throw IllegalStateException("无效的父任务")
            projectId = parent.projectId
        }
        val task = TaskEntity(
            id = UUID.randomUUID().toString(),
            parentId = input.parentId,
            projectId = projectId,
            title = input.title.trim(),
            description = input.description.trim(),
            status = input.status,
            priority = input.priority,
            dueAt = input.dueAt,
            createdAt = now,
            updatedAt = now
        )
        taskDao.upsert(task)
        return task
    }

    suspend fun update(id: String, patch: TaskPatch): TaskEntity? {
        val task = taskDao.get(id) ?: return null
        val now = System.currentTimeMillis()
        var projectId = if (patch.updateProjectId) patch.projectId else task.projectId
        var parentId = if (patch.updateParentId) patch.parentId else task.parentId
        if (patch.updateParentId && patch.parentId != null) {
            val parent = taskDao.get(patch.parentId)
            if (parent == null || parent.parentId != null) throw IllegalStateException("无效的父任务")
            projectId = parent.projectId
            // 该任务降级为子任务，其原有子任务提升为顶级，避免被孤儿清理删除
            for (c in taskDao.getByParent(id)) {
                taskDao.upsert(c.copy(parentId = null, projectId = projectId, updatedAt = now))
            }
        }
        val updated = task.copy(
            title = patch.title?.trim() ?: task.title,
            description = patch.description?.trim() ?: task.description,
            status = patch.status ?: task.status,
            priority = patch.priority ?: task.priority,
            dueAt = if (patch.updateDueAt) patch.dueAt else task.dueAt,
            parentId = parentId,
            projectId = projectId,
            updatedAt = now
        )
        taskDao.upsert(updated)
        // 顶级任务移动项目时同步更新其子任务（与桌面端一致）
        if (patch.updateProjectId && task.parentId == null && parentId == null) {
            for (c in taskDao.getByParent(id)) {
                taskDao.upsert(c.copy(projectId = patch.projectId, updatedAt = now))
            }
        }
        return updated
    }

    /** 完成状态切换（列表快速操作） */
    suspend fun toggleCompleted(id: String): TaskEntity? {
        val task = taskDao.get(id) ?: return null
        val next = if (task.status == TaskConstants.COMPLETED) TaskConstants.IN_PROGRESS else TaskConstants.COMPLETED
        return update(id, TaskPatch(status = next))
    }

    suspend fun setProject(projectId: String?): Unit {
        val now = System.currentTimeMillis()
        for (t in taskDao.all().filter { it.projectId == projectId }) {
            taskDao.upsert(t.copy(projectId = null, updatedAt = now))
        }
    }

    suspend fun remove(id: String): Boolean {
        val task = taskDao.get(id) ?: return false
        val now = System.currentTimeMillis()
        val removed = listOf(task) + taskDao.getByParent(id)
        taskDao.deleteByIds(removed.map { it.id })
        tombstoneDao.upsertAll(
            removed.map { TombstoneEntity(kind = "task", id = it.id, updatedAt = now) }
        )
        return true
    }

    suspend fun applyRemote(tasks: List<TaskDto>, tombstones: List<TombstoneDto>) {
        val byId = taskDao.all().associateBy { it.id }.toMutableMap()
        // 服务端 live 记录：本地没有或时间戳更新则覆盖
        for (t in tasks) {
            val cur = byId[t.id]
            if (cur == null || t.updatedAt >= cur.updatedAt) {
                byId[t.id] = t.toEntity()
            }
        }
        // 服务端墓碑：本地有且较旧则删除
        for (tb in tombstones) {
            if (tb.kind != "task") continue
            val cur = byId[tb.id]
            if (cur != null && tb.updatedAt >= cur.updatedAt) {
                byId.remove(tb.id)
            }
        }
        taskDao.clear()
        taskDao.upsertAll(byId.values.toList())
        // 墓碑以服务端为准
        tombstoneDao.clearKind("task")
        tombstoneDao.upsertAll(tombstones.filter { it.kind == "task" }.map { it.toEntity() })
        cleanupOrphans()
    }

    private suspend fun cleanupOrphans() {
        val all = taskDao.all()
        val ids = all.map { it.id }.toSet()
        val orphans = all.filter { it.parentId != null && it.parentId !in ids }
        if (orphans.isNotEmpty()) {
            val now = System.currentTimeMillis()
            taskDao.deleteByIds(orphans.map { it.id })
            tombstoneDao.upsertAll(
                orphans.map { TombstoneEntity(kind = "task", id = it.id, updatedAt = now) }
            )
        }
    }
}