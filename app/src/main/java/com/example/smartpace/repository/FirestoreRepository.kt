package com.example.smartpace.repository

import com.example.smartpace.model.Friend
import com.example.smartpace.model.FriendRequest
import com.example.smartpace.model.RouteAlert
import com.example.smartpace.model.Run
import com.example.smartpace.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId get() = auth.currentUser?.uid ?: ""

    // ─── Perfil do usuário ───────────────────────────────────────

    suspend fun saveUserProfile(profile: UserProfile) {
        db.collection("users")
            .document(userId)
            .set(profile, SetOptions.merge())
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

    // ─── Username ────────────────────────────────────────────────

    private fun normalizeUsername(raw: String) = raw.lowercase().trim()

    suspend fun getUserProfileById(uid: String): UserProfile? {
        return try {
            db.collection("users").document(uid).get().await()
                .toObject(UserProfile::class.java)?.copy(id = uid)
        } catch (e: Exception) {
            null
        }
    }

    /** Verifica se o username está livre (ou já pertence ao próprio usuário). */
    suspend fun isUsernameAvailable(username: String): Boolean {
        val doc = db.collection("usernames").document(normalizeUsername(username)).get().await()
        return !doc.exists() || doc.getString("uid") == userId
    }

    /**
     * Reserva o username de forma atômica: garante unicidade no índice `usernames`,
     * libera o antigo e grava no perfil. Retorna false se já estiver em uso por outro.
     */
    suspend fun setUsername(username: String): Boolean {
        val normalized = normalizeUsername(username)
        val newRef = db.collection("usernames").document(normalized)
        val userRef = db.collection("users").document(userId)
        return try {
            db.runTransaction { txn ->
                val existing = txn.get(newRef)
                if (existing.exists() && existing.getString("uid") != userId) {
                    throw IllegalStateException("username_taken")
                }
                val oldUsername = txn.get(userRef).getString("username")
                if (!oldUsername.isNullOrEmpty() && oldUsername != normalized) {
                    txn.delete(db.collection("usernames").document(oldUsername))
                }
                txn.set(newRef, mapOf("uid" to userId))
                txn.update(userRef, "username", normalized)
                null
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // ─── Amizades ────────────────────────────────────────────────

    /** Busca um usuário pelo username exato via índice `usernames`. */
    suspend fun searchUserByUsername(username: String): UserProfile? {
        val normalized = normalizeUsername(username)
        if (normalized.isEmpty() || normalized == getUserProfile()?.username) return null
        val doc = db.collection("usernames").document(normalized).get().await()
        val uid = doc.getString("uid") ?: return null
        return getUserProfileById(uid)
    }

    /** Envia solicitação, evitando duplicar uma já pendente. */
    suspend fun sendFriendRequest(toUid: String) {
        val existing = db.collection("friend_requests")
            .whereEqualTo("fromUid", userId)
            .whereEqualTo("toUid", toUid)
            .whereEqualTo("status", "pending")
            .get().await()
        if (!existing.isEmpty) return

        val me = getUserProfile()
        val request = FriendRequest(
            fromUid = userId,
            fromName = me?.name ?: "",
            fromUsername = me?.username ?: "",
            toUid = toUid,
            status = "pending"
        )
        db.collection("friend_requests").add(request).await()
    }

    suspend fun getIncomingRequests(): List<FriendRequest> {
        return try {
            val snap = db.collection("friend_requests")
                .whereEqualTo("toUid", userId)
                .whereEqualTo("status", "pending")
                .get().await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Aceita: marca a solicitação e grava o vínculo nos dois lados (batch). */
    suspend fun acceptFriendRequest(request: FriendRequest) {
        val me = getUserProfile()
        val batch = db.batch()

        val reqRef = db.collection("friend_requests").document(request.id)
        batch.update(reqRef, "status", "accepted")

        val myFriendRef = db.collection("users").document(userId)
            .collection("friends").document(request.fromUid)
        batch.set(myFriendRef, Friend(
            uid = request.fromUid, name = request.fromName, username = request.fromUsername
        ))

        val theirFriendRef = db.collection("users").document(request.fromUid)
            .collection("friends").document(userId)
        batch.set(theirFriendRef, Friend(
            uid = userId, name = me?.name ?: "", username = me?.username ?: ""
        ))

        batch.commit().await()
    }

    suspend fun rejectFriendRequest(requestId: String) {
        db.collection("friend_requests").document(requestId).delete().await()
    }

    suspend fun getFriends(): List<Friend> {
        return try {
            val snap = db.collection("users").document(userId)
                .collection("friends").get().await()
            snap.documents.mapNotNull { it.toObject(Friend::class.java) }
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
