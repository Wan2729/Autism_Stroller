package com.example.autismstroller.models

import com.google.firebase.Timestamp

data class Achievement(
    val id: String = "",
    val iconRes:String = "", // Path to Achievement Icon in Firebase
    val title: String = "",
    val description: String = "",
    val completed: Boolean = false,
    val unlockedAt: Timestamp? = null,
    val stats: List<String> = emptyList(), // Needed for Firestore search
    val unlockCondition: List<AchievementCondition> = emptyList()
){
    companion object {
        fun fromMap(map: Map<String, Any>): Achievement {
            val conditionMaps =
                map["unlockCondition"] as? List<Map<String, Any>> ?: emptyList()

            val conditions = conditionMaps.map {
                AchievementCondition.fromMap(it)
            }

            return Achievement(
                id = map["id"] as? String ?: "",
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                completed = map["completed"] as? Boolean ?: false,
                unlockedAt = map["unlockedAt"] as? Timestamp,
                stats = map["stats"] as? List<String> ?: emptyList(),
                unlockCondition = conditions
            )
        }
    }
}

data class AchievementCondition(
    val stat: String = "",
    val operator: Operator = Operator.EQUAL,
    val value: Int = 0
){
    companion object {
        fun fromMap(map: Map<String, Any>): AchievementCondition {
            return AchievementCondition(
                stat = map["stat"] as? String ?: "",
                operator = Operator.valueOf(
                    map["operator"] as? String ?: Operator.EQUAL.name
                ),
                value = (map["value"] as? Number)?.toInt() ?: 0
            )
        }
    }
}

enum class Operator {
    GREATER_THAN,
    GREATER_OR_EQUAL,
    EQUAL,
    LESSER_THAN,
    LESSER_OR_EQUAL
}

/*
    !! IMPORTANT NOTE !!
    Must be maintain with
    UserStats in User file
 */
data class UserStatDefinition(
    val key: String,
    val displayName: String
)
object UserStatDefinitions {
    val all = listOf(
        UserStatDefinition("followCount", "Follow Count"),
        UserStatDefinition("forumCount", "Forum Count"),
        UserStatDefinition("likeCount", "Like Count"),
        UserStatDefinition("turnOnLEDCount", "LED Turn On Count"),
        UserStatDefinition("currentLoginStreak", "Login Streak"),
        UserStatDefinition("timeSpendOnForum","Time Spend on Forum")
    )
}
