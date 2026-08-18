package com.mission.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mission.app.data.entity.TombstoneEntity

@Dao
interface TombstoneDao {
    @Query("SELECT * FROM tombstones")
    suspend fun all(): List<TombstoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tombstone: TombstoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tombstones: List<TombstoneEntity>)

    @Query("DELETE FROM tombstones WHERE kind = :kind")
    suspend fun clearKind(kind: String)

    @Query("DELETE FROM tombstones WHERE kind = :kind AND id IN (:ids)")
    suspend fun deleteByKindAndIds(kind: String, ids: List<String>)

    @Query("DELETE FROM tombstones")
    suspend fun clear()
}