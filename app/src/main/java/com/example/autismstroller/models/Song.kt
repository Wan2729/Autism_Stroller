package com.example.autismstroller.models

import com.google.firebase.firestore.PropertyName

data class Song(
    val id: String = "",
    val name: String = "",
    val url: String = "", // This will be the Supabase Public URL
    val listenCount: Int = 0,
    val timeCreated: Long = 0,

    @get:PropertyName("isPublic")
    @set:PropertyName("isPublic")
    var isPublic: Boolean = false,

    val ownerId: String = ""
)