package com.example.autismstroller.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Message (
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
    val status: MessageStatus? = MessageStatus.SENT,
    @get:PropertyName("isEncrypted")
    @set:PropertyName("isEncrypted")
    var isEncrypted: Boolean = false
)

enum class MessageStatus {
    SENT,
    DELIVERED,
    SEEN
}