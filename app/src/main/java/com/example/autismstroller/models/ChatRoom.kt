package com.example.autismstroller.models

import com.google.firebase.Timestamp

data class ChatRoom(
    val id: String = "",
    val participantsId: List<String> = listOf("",""),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val lastUpdate: Timestamp? = null,
    val lastMessageIsEncrypted: Boolean = false
)