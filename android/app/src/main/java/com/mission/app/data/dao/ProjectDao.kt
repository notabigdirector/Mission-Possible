package com.mission.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mission.app.data.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects")
    suspend fun all(): List<ProjectEntity>

    @Query("SELECT * FROM projects")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun get(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(projects: List<ProjectEntity>)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM projects")
    suspend fun clear()
}