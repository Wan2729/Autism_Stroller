package com.example.autismstroller.models

import com.google.firebase.Timestamp

data class Forum(
    val id: String = "",
    val topic: String = "",
    val text: String = "",
    val like: Int = 0,
    val sender: String = "",
    val timestamp: Timestamp? = null
)