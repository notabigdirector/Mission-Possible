package com.mission.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.mission.app.data.AppDatabase
import com.mission.app.data.repository.ProjectRepository
import com.mission.app.data.repository.SyncRepository
import com.mission.app.data.repository.TaskRepository
import com.mission.app.sync.SyncManager
import com.mission.app.sync.SyncPrefs

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "mission.db").build()
    }
    val syncPrefs: SyncPrefs by lazy { SyncPrefs(appContext) }
    val taskRepository: TaskRepository by lazy {
        TaskRepository(database.taskDao(), database.tombstoneDao())
    }
    val projectRepository: ProjectRepository by lazy {
        ProjectRepository(database.projectDao(), database.tombstoneDao())
    }
    val syncRepository: SyncRepository by lazy {
        SyncRepository(taskRepository, projectRepository)
    }
    val syncManager: SyncManager by lazy {
        SyncManager(syncPrefs, syncRepository, appContext)
    }
}

class MissionApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.syncManager.start()
    }
}