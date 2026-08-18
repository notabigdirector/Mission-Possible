package com.mission.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mission.app.data.dao.ProjectDao
import com.mission.app.data.dao.TaskDao
import com.mission.app.data.dao.TombstoneDao
import com.mission.app.data.entity.ProjectEntity
import com.mission.app.data.entity.TaskEntity
import com.mission.app.data.entity.TombstoneEntity

@Database(
    entities = [TaskEntity::class, ProjectEntity::class, TombstoneEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun tombstoneDao(): TombstoneDao
}