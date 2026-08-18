package com.mission.app.data.repository

import com.mission.app.data.dao.ProjectDao
import com.mission.app.data.dao.TombstoneDao
import com.mission.app.data.entity.ProjectEntity
import com.mission.app.data.entity.TombstoneEntity
import com.mission.app.data.model.ProjectDto
import com.mission.app.data.model.TombstoneDto
import com.mission.app.data.model.toDto
import com.mission.app.data.model.toEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class ProjectInput(
    val name: String,
    val priority: Int
)

/**
 * 对齐 src/main/project-store.ts 的本地存储语义。
 */
class ProjectRepository(
    private val projectDao: ProjectDao,
    private val tombstoneDao: TombstoneDao
) {
    fun observeAll(): Flow<List<ProjectEntity>> = projectDao.observeAll()

    suspend fun all(): List<ProjectEntity> = projectDao.all()

    suspend fun get(id: String): ProjectEntity? = projectDao.get(id)

    suspend fun tombstones(): List<TombstoneEntity> = tombstoneDao.all().filter { it.kind == "project" }

    suspend fun create(input: ProjectInput): ProjectEntity {
        val now = System.currentTimeMillis()
        val project = ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = input.name.trim(),
            priority = input.priority,
            createdAt = now,
            updatedAt = now
        )
        projectDao.upsert(project)
        return project
    }

    suspend fun update(id: String, name: String, priority: Int): ProjectEntity? {
        val project = projectDao.get(id) ?: return null
        val updated = project.copy(
            name = name.trim(),
            priority = priority,
            updatedAt = System.currentTimeMillis()
        )
        projectDao.upsert(updated)
        return updated
    }

    suspend fun remove(id: String): Boolean {
        val project = projectDao.get(id) ?: return false
        projectDao.deleteById(id)
        tombstoneDao.upsert(
            TombstoneEntity(kind = "project", id = id, updatedAt = System.currentTimeMillis())
        )
        return true
    }

    suspend fun applyRemote(projects: List<ProjectDto>, tombstones: List<TombstoneDto>) {
        val byId = projectDao.all().associateBy { it.id }.toMutableMap()
        for (p in projects) {
            val cur = byId[p.id]
            if (cur == null || p.updatedAt >= cur.updatedAt) {
                byId[p.id] = p.toEntity()
            }
        }
        for (tb in tombstones) {
            if (tb.kind != "project") continue
            val cur = byId[tb.id]
            if (cur != null && tb.updatedAt >= cur.updatedAt) {
                byId.remove(tb.id)
            }
        }
        projectDao.clear()
        projectDao.upsertAll(byId.values.toList())
        tombstoneDao.clearKind("project")
        tombstoneDao.upsertAll(tombstones.filter { it.kind == "project" }.map { it.toEntity() })
    }
}