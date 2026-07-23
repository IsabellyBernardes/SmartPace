package com.example.smartpace.model

data class FriendRequest(
    val id: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val fromUsername: String = "",
    val toUid: String = "",
    val status: String = "pending", // pending | accepted | rejected
    val createdAt: Long = System.currentTimeMillis()
)

data class Friend(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val since: Long = System.currentTimeMillis()
)
