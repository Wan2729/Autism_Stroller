package com.example.autismstroller.functional

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autismstroller.models.User
import com.example.autismstroller.models.UserStats
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UserHandler(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {
    private val statsCollection = firestore.collection("users")
    private val _userState = MutableStateFlow<UserState>(UserState.NoAction)
    val userstate: StateFlow<UserState> = _userState

    fun addFriend(
        myUid: String,
        friendUid: String,
        onSuccess: (Int) -> Unit // <--- Add this parameter
    ) {
        _userState.value = UserState.Loading
        val userRef = firestore.collection("users").document(myUid)

        userRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val currentFriends = snapshot.get("friends") as? List<String> ?: emptyList()

                    if (!currentFriends.contains(friendUid)) {
                        val updatedFriends = currentFriends + friendUid

                        // Calculate the new count immediately
                        val newCount = updatedFriends.size

                        userRef.update("friends", updatedFriends)
                            .addOnSuccessListener {
                                // 1. Update the general state (UI stops loading)
                                _userState.value = UserState.Success("Friend added")

                                // 2. Trigger the specific callback with the count
                                onSuccess(newCount)
                            }
                            .addOnFailureListener { e ->
                                _userState.value = UserState.Error("Failed to add friend: ${e.message}")
                            }
                    } else {
                        _userState.value = UserState.Error("Already friends")
                    }
                }
            }
            .addOnFailureListener { e ->
                _userState.value = UserState.Error("Failed to fetch user data: ${e.message}")
            }
    }

    suspend fun getUserDetailsSuspend(uid: String){
        val userRef = firestore.collection("users").document(uid)

        firestore.runTransaction { tx ->
            val snap = tx.get(userRef)
            if (!snap.contains("userStats")) {
                tx.update(userRef, "userStats", mapOf(
                    "followCount" to 0,
                    "forumCount" to 0,
                    "likeCount" to 0,
                    "turnOnLEDCount" to 0,
                    "currentLoginStreak" to 0,
                    "lastLoginDate" to ""
                ))
            }
        }.await()

        userRef.get()
            .addOnSuccessListener {snapshot ->
                if(snapshot.exists()){
                    val theUser = snapshot.toObject<User>()?.copy(uid = snapshot.id)
                    _userState.value = UserState.UserDetails(theUser)
                }
            }
    }

    suspend fun increment(userId: String, stat: String, amount: Int = 1) {
        val userRef = statsCollection.document(userId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val current = (snapshot.getLong("userStats.$stat") ?: 0L) + amount
            transaction.update(userRef, "userStats.$stat", current)
        }.await()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateLoginStreak(userId: String) {
        val userRef = firestore.collection("users").document(userId)
        val formatter = DateTimeFormatter.ISO_DATE
        val today = LocalDate.now()

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val statsMap = snapshot.get("userStats") as? Map<String, Any> ?: emptyMap()

            val lastLoginStr = statsMap["lastLoginDate"] as? String ?: ""
            val currentStreak = (statsMap["currentLoginStreak"] as? Number)?.toInt() ?: 0

            val newStreak = if (lastLoginStr.isNotEmpty()) {
                val lastLogin = LocalDate.parse(lastLoginStr, formatter)
                when {
                    lastLogin == today.minusDays(1) -> currentStreak + 1   // consecutive day
                    lastLogin == today -> currentStreak                     // already logged in today
                    else -> 1                                              // missed day
                }
            } else {
                1 // first login
            }

            // Update the streak and last login date inside the userStats map
            transaction.update(userRef, mapOf(
                "userStats.currentLoginStreak" to newStreak,
                "userStats.lastLoginDate" to today.format(formatter)
            ))
        }.await()
    }

    suspend fun getStats(userId: String): UserStats? {
        return try {
            val snapshot = statsCollection.document(userId).get().await()
            if (snapshot.exists()) {
                UserStats(
                    uid = userId ?: "",
                    followCount = snapshot.getLong("followCount")?.toInt() ?: 0,
                    forumCount = snapshot.getLong("postCount")?.toInt() ?: 0,
                    likeCount = snapshot.getLong("likeCount")?.toInt() ?: 0,
                    currentLoginStreak = snapshot.getLong("currentLoginStreak")?.toInt() ?: 0,
                    lastLoginDate = snapshot.getString("lastLoginDate") ?: ""
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getStatValue(userId: String, stat: String): Int {
        return try {
            val snapshot = firestore.collection("users").document(userId).get().await()
            val statsMap = snapshot.get("userStats") as? Map<String, Any> ?: emptyMap()
            (statsMap[stat] as? Number)?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun increaseLevel(uid: String, amount: Int){
        val userRef = statsCollection.document(uid)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val current = (snapshot.getLong("level") ?: 0L) + amount
            transaction.update(userRef, "level", current)
        }.await()
    }

    fun updateUserProfile(
        userId: String,
        name: String,
        age: Int,              // Changed to Int
        genderMale: Boolean,   // Changed to Boolean
        description: String,
        profilePictureBase64: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userRef = firestore.collection("users").document(userId)

        // Create map matching your Firestore document structure exactly
        val updates = mapOf(
            "name" to name,
            "age" to age,
            "genderMale" to genderMale,
            "description" to description,
            "profilePicture" to profilePictureBase64
        )

        userRef.update(updates)
            .addOnSuccessListener {
                // Refresh local data so UI updates immediately
                getUserDetails(userId)
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Failed to update profile")
            }
    }

    fun getUserDetails(uid: String) {
        viewModelScope.launch {
            getUserDetailsSuspend(uid)
        }
    }

    // Add this inside UserHandler class
    fun checkIsFriend(myUid: String, friendUid: String, onResult: (Boolean) -> Unit) {
        val userRef = firestore.collection("users").document(myUid)
        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val myFriends = snapshot.get("friends") as? List<String> ?: emptyList()
                // Check if *I* have added *HIM*
                onResult(myFriends.contains(friendUid))
            } else {
                onResult(false)
            }
        }.addOnFailureListener {
            onResult(false)
        }
    }
}

sealed class UserState {
    object NoAction : UserState()
    data class UserDetails(val userDetail : User?) : UserState()
    data class Success(val message: String): UserState()
    object Loading : UserState()
    data class Error(val message: String) : UserState()
}