package com.example.smartpace.repository

import android.content.Context
import com.example.smartpace.db.local.LocalDatabase
import com.example.smartpace.db.local.toLocalRun
import com.example.smartpace.db.local.toRun
import com.example.smartpace.model.Run
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RunRepository(context: Context) {

    private val firestore = FirestoreRepository()
    private val local = LocalDatabase(context.applicationContext, DATABASE_NAME)

    fun runs(): Flow<List<Run>> = local.getRuns().map { list -> list.map { it.toRun() } }

    suspend fun refresh(): List<Run> {
        val remote = firestore.getRuns()
        local.replaceAll(remote.map { it.toLocalRun() })
        return remote
    }

    suspend fun save(run: Run) {
        val id = firestore.saveRun(run)
        local.upsert(run.copy(id = id).toLocalRun())
    }

    companion object {
        private const val DATABASE_NAME = "smartpace_runs.db"
    }
}
