package com.example.smartpace.model

/** Solicitação de amizade entre dois usuários. */
data class FriendRequest(
    val id: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val fromUsername: String = "",
    val toUid: String = "",
    val status: String = "pending", // pending | accepted | rejected
    val createdAt: Long = System.currentTimeMillis()
)

/** Amigo já aceito, guardado na subcoleção do usuário. */
data class Friend(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val since: Long = System.currentTimeMillis()
)
