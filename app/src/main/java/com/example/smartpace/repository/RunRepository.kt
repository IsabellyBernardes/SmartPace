package com.example.smartpace.repository

import android.content.Context
import com.example.smartpace.db.local.LocalDatabase
import com.example.smartpace.db.local.toLocalRun
import com.example.smartpace.db.local.toRun
import com.example.smartpace.model.Run
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Abstrai o armazenamento das corridas para o restante do app. As corridas são
 * salvas no Firestore (fonte da verdade) e espelhadas no Room (fonte local, que a
 * UI observa). Assim o histórico aparece instantaneamente e funciona offline.
 */
class RunRepository(context: Context) {

    private val firestore = FirestoreRepository()
    private val local = LocalDatabase(context.applicationContext, DATABASE_NAME)

    /** Fluxo reativo das corridas locais — a UI observa isto. */
    fun runs(): Flow<List<Run>> = local.getRuns().map { list -> list.map { it.toRun() } }

    /** Puxa do Firestore e espelha no Room. Devolve a lista para uso imediato. */
    suspend fun refresh(): List<Run> {
        val remote = firestore.getRuns()
        local.replaceAll(remote.map { it.toLocalRun() })
        return remote
    }

    /** Salva no Firestore (que gera o id) e espelha no Room. */
    suspend fun save(run: Run) {
        val id = firestore.saveRun(run)
        local.upsert(run.copy(id = id).toLocalRun())
    }

    companion object {
        private const val DATABASE_NAME = "smartpace_runs.db"
    }
}
