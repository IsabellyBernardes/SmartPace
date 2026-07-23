package com.example.smartpace.db.local

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow

class LocalDatabase(context: Context, databaseName: String) {

    private val roomDB = Room.databaseBuilder(
        context = context,
        klass = LocalRoomDatabase::class.java,
        name = databaseName
    ).build()

    private val dao = roomDB.runDao()

    fun getRuns(): Flow<List<LocalRun>> = dao.getRuns()

    suspend fun upsert(run: LocalRun) = dao.upsert(run)

    suspend fun replaceAll(runs: List<LocalRun>) {
        dao.clear()
        dao.upsertAll(runs)
    }
}
