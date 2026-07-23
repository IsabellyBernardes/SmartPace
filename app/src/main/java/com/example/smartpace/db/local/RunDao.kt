package com.example.smartpace.db.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {

    @Upsert
    suspend fun upsert(run: LocalRun)

    @Upsert
    suspend fun upsertAll(runs: List<LocalRun>)

    @Query("DELETE FROM runs")
    suspend fun clear()

    @Query("SELECT * FROM runs ORDER BY timestamp DESC")
    fun getRuns(): Flow<List<LocalRun>>
}
