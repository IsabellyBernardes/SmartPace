package com.example.smartpace.repository

import com.example.smartpace.model.RouteAlert
import com.example.smartpace.model.Run
import com.example.smartpace.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId get() = auth.currentUser?.uid ?: ""

    // ─── Perfil do usuário ───────────────────────────────────────

    suspend fun saveUserProfile(profile: UserProfile) {
        db.collection("users")
            .document(userId)
            .set(profile)
            .await()
    }

    suspend fun getUserProfile(): UserProfile? {
        return try {
            val doc = db.collection("users")
                .document(userId)
                .get()
                .await()
            doc.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createUserProfileIfNotExists(name: String, email: String) {
        val doc = db.collection("users").document(userId).get().await()
        if (!doc.exists()) {
            val profile = UserProfile(
                id = userId,
                name = name,
                email = email,
                memberSince = getCurrentMonthYear(),
                weeklyGoalKm = 50.0
            )
            saveUserProfile(profile)
        }
    }

    // ─── Corridas ────────────────────────────────────────────────

    suspend fun saveRun(run: Run): String {
        val docRef = db.collection("users")
            .document(userId)
            .collection("runs")
            .add(run)
            .await()
        return docRef.id
    }

    suspend fun getRuns(): List<Run> {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("runs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Run::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteRun(runId: String) {
        db.collection("users")
            .document(userId)
            .collection("runs")
            .document(runId)
            .delete()
            .await()
    }

    // ─── Alertas comunitários ─────────────────────────────────────

    suspend fun saveAlert(alert: RouteAlert) {
        db.collection("alerts")
            .add(alert)
            .await()
    }

    suspend fun getAlerts(): List<RouteAlert> {
        return try {
            val snapshot = db.collection("alerts").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(RouteAlert::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ─── Utilitários ─────────────────────────────────────────────

    private fun getCurrentMonthYear(): String {
        val cal = java.util.Calendar.getInstance()
        val month = arrayOf(
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
            "Jul", "Ago", "Set", "Out", "Nov", "Dez"
        )[cal.get(java.util.Calendar.MONTH)]
        return "$month ${cal.get(java.util.Calendar.YEAR)}"
    }
}
