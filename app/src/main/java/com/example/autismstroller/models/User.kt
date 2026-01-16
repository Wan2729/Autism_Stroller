package com.example.autismstroller.models

import java.time.LocalDate

data class User(
    val uid: String = "", // Unique identifier
    val name: String = "",
    val age: Int = 0,
    val genderMale: Boolean = false,
    val type: String = "",
    val description: String = "",

    val email: String = "",
    val phoneNumber: String = "",

    val friends: List<String> = listOf(), // Store user IDs instead of full User objects
    val level: Int = 0,
    val achievements: List<Achievement> = listOf(), // Use custom Achievement class

    val profilePicture: String = "", // Path to Profile Picture Photo in Firebase

    val userStats: UserStats = UserStats()
)

/*
    !! IMPORTANT NOTE !!
    Must be maintain with
    UserStatDefinitions in Achievement file
 */
data class UserStats(
    val uid: String = "",  // X
    val followCount: Int = 0,  // V
    val forumCount: Int = 0,  // V
    val likeCount: Int = 0,  // V
    val turnOnLEDCount: Int = 0,  // V
    val currentLoginStreak: Int = 0,  // V
    val lastLoginDate: String = "",  // X
    val timeSpendOnForum: Int = 0 // V
){
    companion object {
        fun fromMap(uid: String, map: Map<String, Any>?): UserStats {
            val statsMap = map ?: emptyMap()
            return UserStats(
                uid = uid,
                followCount = (statsMap["followCount"] as? Number)?.toInt() ?: 0,
                forumCount = (statsMap["forumCount"] as? Number)?.toInt() ?: 0,
                likeCount = (statsMap["likeCount"] as? Number)?.toInt() ?: 0,
                turnOnLEDCount = (statsMap["turnOnLEDCount"] as? Number)?.toInt() ?: 0,
                currentLoginStreak = (statsMap["currentLoginStreak"] as? Number)?.toInt() ?: 0,
                lastLoginDate = statsMap["lastLoginDate"] as? String ?: "",
                timeSpendOnForum = (statsMap["timeSpendOnForum"] as? Number)?.toInt() ?: 0
            )
        }
    }
}