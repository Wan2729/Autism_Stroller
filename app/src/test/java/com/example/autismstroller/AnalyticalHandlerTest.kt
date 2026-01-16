package com.example.autismstroller.functional

import com.example.autismstroller.models.Child
import com.example.autismstroller.models.ChildStats
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticalHandlerTest {

    // 1. Setup the class we are testing
    private val handler = AnalyticalHandler()

    @Test
    fun `processStat Music sorts top 5 songs correctly`() = runTest {
        // GIVEN: A Child object with specific music stats
        val musicData = mapOf(
            "Baby Shark" to 50,
            "Twinkle Star" to 10,
            "Let it Go" to 100, // Should be #1
            "Song D" to 5,
            "Song E" to 2,
            "Song F" to 1 // Should be dropped (only top 5 kept)
        )

        // Create dummy stats (Fill other fields with defaults)
        val dummyStats = ChildStats(musicPreferences = musicData)
        val dummyChild = Child(id = "1", childStats = dummyStats)

        // WHEN: We process the "Music" category
        handler.processStat(dummyChild, "Music")

        // THEN: Check the results in the StateFlow
        val results = handler.graphData.value

        assertEquals("Should have 5 items", 5, results.size)
        assertEquals("First item should be 'Let it Go'", "Let it Go", results[0].label)
        assertEquals("Second item should be 'Baby Shark'", "Baby Shark", results[1].label)
    }

    @Test
    fun `processStat Lights maps colors to correct Hex codes`() = runTest {
        // GIVEN: A child with light usage
        val colorData = mapOf("Red" to 10, "Blue" to 5)
        val dummyStats = ChildStats(colorPreferences = colorData)
        val dummyChild = Child(id = "1", childStats = dummyStats)

        // WHEN: We process "Lights"
        handler.processStat(dummyChild, "Lights")

        // THEN
        val results = handler.graphData.value
        val redPoint = results.find { it.label == "Red" }

        // 0xFFFF6B6B is the Long value for the Red hex used in your handler
        assertEquals(0xFFFF6B6B, redPoint?.colorHex?.toLong())
    }
}