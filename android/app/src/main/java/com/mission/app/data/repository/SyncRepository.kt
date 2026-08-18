package com.mission.app.data.repository

import com.mission.app.data.model.SyncRequest
import com.mission.app.data.model.SyncResponse
import com.mission.app.data.model.toDto

class SyncRepository(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository
) {
    suspend fun buildRequest(): SyncRequest = SyncRequest(
        tasks = taskRepository.all().map { it.toDto() },
        projects = projectRepository.all().map { it.toDto() },
        deleted = (taskRepository.tombstones() + projectRepository.tombstones()).map { it.toDto() }
    )

    suspend fun applyResponse(resp: SyncResponse) {
        taskRepository.applyRemote(resp.tasks, resp.tombstones)
        projectRepository.applyRemote(resp.projects, resp.tombstones)
    }
}