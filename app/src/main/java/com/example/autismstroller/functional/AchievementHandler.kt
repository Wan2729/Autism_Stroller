package com.example.autismstroller.functional

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.autismstroller.models.Achievement
import com.example.autismstroller.models.AchievementCondition
import com.example.autismstroller.models.Operator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AchievementHandler (
    private val userHandler: UserHandler,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
): ViewModel() {

    suspend fun getAchievement(id: String): Achievement? {
        return try {
            val snapshot = firestore
                .collection("achievements")
                .document(id)
                .get()
                .await()

            if (!snapshot.exists()) return null

            Achievement.fromMap(snapshot.data!!)
        } catch (e: Exception) {
            Log.e("AchievementHandler", "Failed to get achievement $id", e)
            null
        }
    }

    suspend fun getAchievementByUser(uid: String): List<Achievement> {
        return try {
            val snapshot = firestore
                .collection("users")
                .document(uid)
                .get()
                .await()

            val achievementsArray =
                snapshot.get("achievements") as? List<Map<String, Any>> ?: return emptyList()

            achievementsArray.mapNotNull { map ->
                try {
                    Achievement.fromMap(map)
                } catch (e: Exception) {
                    Log.e("ACHIEVEMENT", "Failed to parse achievement: $map", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ACHIEVEMENT", "getAchievementByUser failed", e)
            emptyList()
        }
    }

    suspend fun getAllAchievement(): List<Achievement> {
        return try {
            val snapshot = firestore.collection("achievements").get().await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val title = doc.getString("title") ?: ""
                    val description = doc.getString("description") ?: ""
                    val iconRes = doc.getString("iconRes") ?: ""
                    val completed = doc.getBoolean("completed") ?: false
                    val unlockedAt = doc.getTimestamp("unlockedAt")

                    // Convert Firestore array of maps to List<AchievementCondition>
                    val unlockConditionList = (doc.get("unlockCondition") as? List<Map<String, Any>>)?.map { map ->
                        AchievementCondition(
                            stat = map["stat"] as? String ?: "",
                            operator = try {
                                Operator.valueOf(map["operator"] as? String ?: "EQUAL")
                            } catch (e: Exception) {
                                Operator.EQUAL // fallback if value is invalid
                            },
                            value = (map["value"] as? Long)?.toInt() ?: 0
                        )
                    } ?: emptyList()

                    Achievement(
                        id = id,
                        iconRes = iconRes,
                        title = title,
                        description = description,
                        completed = completed,
                        unlockedAt = unlockedAt,
                        unlockCondition = unlockConditionList
                    )
                } catch (e: Exception) {
                    Log.e("AchievementHandler", "Error parsing achievement ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("AchievementHandler", "Failed to get all achievement", e)
            emptyList()
        }
    }

    suspend fun getAchievementsByStat(stat: String): List<Achievement> {
        return try {
            val snapshot = firestore
                .collection("achievements")
                .whereArrayContains("stats", stat)
                .whereEqualTo("completed", false)
                .get()
                .await()


            snapshot.documents.mapNotNull {
                it.data?.let(Achievement::fromMap)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun evaluate(userId: String, achievement: Achievement): Boolean {
        return achievement.unlockCondition.all { condition ->
            val statValue = userHandler.getStatValue(userId, condition.stat)
            compare(statValue, condition)
        }
    }

    private fun compare(value: Int, condition: AchievementCondition): Boolean {
        return when (condition.operator) {
            Operator.GREATER_THAN -> value > condition.value
            Operator.GREATER_OR_EQUAL -> value >= condition.value
            Operator.EQUAL -> value == condition.value
            Operator.LESSER_THAN -> value < condition.value
            Operator.LESSER_OR_EQUAL -> value <= condition.value
        }
    }

    suspend fun unlock(userId: String, achievement: Achievement) {
        val userRef = firestore.collection("users").document(userId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)

            // Get current achievements array
            val currentAchievements = snapshot.get("achievements") as? List<Map<String, Any>> ?: emptyList()
            val currentLevel = snapshot.getLong("level") ?: 0L

            // Check if achievement is already unlocked
            val alreadyUnlocked = currentAchievements.any { it["id"] == achievement.id && (it["completed"] as? Boolean == true) }
            if (alreadyUnlocked) return@runTransaction

            // Create map for new achievement
            val newAchievementData = mapOf(
                "id" to achievement.id,
                "iconRes" to achievement.iconRes,
                "title" to achievement.title,
                "description" to achievement.description,
                "completed" to true,
                "unlockedAt" to Timestamp.now(),
                "unlockCondition" to achievement.unlockCondition.map { condition ->
                    mapOf(
                        "stat" to condition.stat,
                        "operator" to condition.operator.name,
                        "value" to condition.value
                    )
                }
            )

            // Add new achievement to array
            val updatedAchievements = currentAchievements + newAchievementData

            // Update Firestore
            transaction.update(userRef, "achievements", updatedAchievements)
            transaction.update(userRef, "level", currentLevel + 1)
            Log.d("Achievement Added", "unlock: New Achievement unlocked: ${achievement.id}")
        }.await()
    }

    suspend fun isUnlocked(userId: String, achievementId: String): Boolean {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val achievementsMap = snapshot.get("achievements") as? Map<String, Any> ?: emptyMap()

            // Check if achievement exists and is completed
            val achievementData = achievementsMap[achievementId] as? Map<String, Any>
            achievementData?.get("completed") as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    // Return type changed from Unit to List<Achievement>
    suspend fun checkAndUnlockByStat(uid: String, stat: String): List<Achievement> {
        val achievements = getAchievementsByStat(stat)
        val newlyUnlocked = mutableListOf<Achievement>() // 1. Create a list to track success

        for (achievement in achievements) {
            // Skip if already unlocked
            if (isUnlocked(uid, achievement.id)) continue

            val unlocked = achievement.unlockCondition.all { condition ->
                val value = userHandler.getStatValue(uid, condition.stat)
                compare(value, condition)
            }

            if (unlocked) {
                unlock(uid, achievement)
                newlyUnlocked.add(achievement) // 2. Add to list if successful
            }
        }

        return newlyUnlocked // 3. Return the result
    }

    fun createAchievement(
        achievement: Achievement,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        firestore
            .collection("achievements")
            .document(achievement.id)
            .set(achievement)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}

class AchievementHandlerFactory(
    private val userHandler: UserHandler
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AchievementHandler::class.java)) {
            return AchievementHandler(userHandler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
