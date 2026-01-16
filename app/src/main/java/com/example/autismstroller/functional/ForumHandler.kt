package com.example.autismstroller.functional

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.example.autismstroller.models.Forum
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ForumHandler(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {
    private val _forumState = MutableStateFlow<ForumState>(ForumState.NoAction)
    val forumstate: StateFlow<ForumState> = _forumState
    private val _forumList = MutableStateFlow<List<Forum>>(emptyList())
    val forumList: StateFlow<List<Forum>> = _forumList.asStateFlow()

    fun create(
        topic: String,
        text: String
    ) {
        _forumState.value = ForumState.Loading
        val uid = auth.currentUser?.uid ?: ""
        if (uid.equals("")) {
            _forumState.value = ForumState.Error("User is not Authenticated")
        } else {
            val timeCreated = Timestamp.now()
            val forumId = "${uid}_${timeCreated.seconds}_${timeCreated.nanoseconds}"
            val newForum = Forum(
                id = forumId,
                topic = topic,
                text = text,
                like = 0,
                sender = uid,
                timestamp = timeCreated

            )
            firestore.collection("forums").document(newForum.id).set(newForum)
                .addOnSuccessListener {
                    _forumList.update { it + newForum }
                    getForum()
                }.addOnFailureListener {exception ->
                    _forumState.value = ForumState.Error("Failed to add Forum: ${exception.message}")
                }
        }
    }

    fun getForum(){
        _forumState.value = ForumState.Loading

        firestore.collection("forums").orderBy("timestamp", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { query ->
                val forums = query.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Forum::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null // Skip malformed documents
                    }
                }
                _forumState.value = ForumState.Success(forums)
            }
            .addOnFailureListener { exception ->
                _forumState.value = ForumState.Error(exception.message ?: "Failed to fetch forums")
            }
    }

    fun clearError(){
        _forumState.value = ForumState.NoAction
    }

    fun getForum(currentUserId: String){
        _forumState.value = ForumState.Loading

        firestore.collection("users").document(currentUserId).get()
            .addOnSuccessListener { userDoc ->
                val friends = userDoc.get("friends") as? List<String>

                if (friends.isNullOrEmpty()) {
                    _forumState.value = ForumState.Success(emptyList()) // No friends = no forums
                    return@addOnSuccessListener
                }

                val limitedFriends = friends.take(10)

                firestore.collection("forums")
                    .whereIn("sender", limitedFriends)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { query ->
                        val forums = query.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Forum::class.java)?.copy(id = doc.id)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _forumState.value = ForumState.Success(forums)
                    }
                    .addOnFailureListener { e ->
                        _forumState.value = ForumState.Error(e.message ?: "Failed to fetch forums")
                    }

            }
            .addOnFailureListener { e ->
                _forumState.value = ForumState.Error(e.message ?: "Failed to fetch friends")
            }
    }
}

sealed class ForumState {
    object NoAction : ForumState()
    data class Success(val forums: List<Forum>): ForumState()
    object Loading : ForumState()
    data class Error(val message: String) : ForumState()
}