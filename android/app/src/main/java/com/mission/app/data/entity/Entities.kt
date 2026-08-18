package com.mission.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

object TaskConstants {
    const val NOT_STARTED = "not_started"
    const val IN_PROGRESS = "in_progress"
    const val TESTING = "testing"
    const val COMPLETED = "completed"

    const val PRIORITY_LOW = "low"
    const val PRIORITY_MEDIUM = "medium"
    const val PRIORITY_HIGH = "high"

    val STATUSES = listOf(NOT_STARTED, IN_PROGRESS, TESTING, COMPLETED)
    val STATUS_LABELS = mapOf(
        NOT_STARTED to "未开始",
        IN_PROGRESS to "进行中",
        TESTING to "测试中",
        COMPLETED to "已完成"
    )
    val PRIORITY_LABELS = mapOf(
        PRIORITY_LOW to "低",
        PRIORITY_MEDIUM to "中",
        PRIORITY_HIGH to "高"
    )
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val parentId: String?,
    val projectId: String?,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val dueAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val priority: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "tombstones",
    primaryKeys = ["kind", "id"]
)
data class TombstoneEntity(
    val kind: String,
    val id: String,
    val updatedAt: Long
)