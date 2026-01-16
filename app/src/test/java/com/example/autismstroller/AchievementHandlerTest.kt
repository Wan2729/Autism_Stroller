package com.example.autismstroller.functional

import com.example.autismstroller.models.Achievement
import com.example.autismstroller.models.AchievementCondition
import com.example.autismstroller.models.Operator
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementHandlerTest {

    // Mock the UserHandler since AchievementHandler needs data from it
    private val mockUserHandler: UserHandler = mock()
    // 1. Create a fake Firestore
    private val mockFirestore: FirebaseFirestore = mock()
    // 2. Inject it into the handler
    private val handler = AchievementHandler(mockUserHandler, mockFirestore)

    @Test
    fun `evaluate returns TRUE when condition is met`() = runTest {
        // GIVEN: An achievement requires Distance > 50
        val condition = AchievementCondition(
            stat = "totalDistance",
            operator = Operator.GREATER_THAN,
            value = 50
        )
        val achievement = Achievement(
            id = "test_ach",
            unlockCondition = listOf(condition)
        )

        // The user actually has 100 distance
        whenever(mockUserHandler.getStatValue("user1", "totalDistance"))
            .doReturn(100)

        // WHEN
        val result = handler.evaluate("user1", achievement)

        // THEN
        assertTrue("Achievement should be unlocked", result)
    }

    @Test
    fun `evaluate returns FALSE when condition is NOT met`() = runTest {
        // GIVEN: An achievement requires Distance > 50
        val condition = AchievementCondition(
            stat = "totalDistance",
            operator = Operator.GREATER_THAN,
            value = 50
        )
        val achievement = Achievement(
            id = "test_ach",
            unlockCondition = listOf(condition)
        )

        // The user only has 10 distance
        whenever(mockUserHandler.getStatValue("user1", "totalDistance"))
            .doReturn(10)

        // WHEN
        val result = handler.evaluate("user1", achievement)

        // THEN
        assertFalse("Achievement should remain locked", result)
    }

    @Test
    fun `evaluate works with EQUAL operator`() = runTest {
        // GIVEN: Requires Level == 5
        val condition = AchievementCondition("level", Operator.EQUAL, 5)
        val achievement = Achievement(id = "lvl5", unlockCondition = listOf(condition))

        whenever(mockUserHandler.getStatValue("user1", "level")).doReturn(5)

        // WHEN
        val result = handler.evaluate("user1", achievement)

        // THEN
        assertTrue(result)
    }
}