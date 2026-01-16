package com.example.autismstroller.functional

import androidx.lifecycle.ViewModel
import com.example.autismstroller.models.Child
import com.example.autismstroller.models.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Simple data class to make graphing easy
data class GraphPoint(
    val label: String,
    val value: Float,
    val colorHex: Long = 0xFF239CA1 // Default teal
)

class AnalyticalHandler : ViewModel() {
    // We reuse the logic to fetch children, or you can pass the list from the screen
    // For simplicity, let's assume the screen passes the selected Child object

    private val _graphData = MutableStateFlow<List<GraphPoint>>(emptyList())
    val graphData: StateFlow<List<GraphPoint>> = _graphData

    private val _summaryText = MutableStateFlow("")
    val summaryText: StateFlow<String> = _summaryText

    // Categories available to select
    val statCategories = listOf("Music", "Lights", "Time of Day", "Overview")

    fun processStat(child: Child, category: String, allSongs: List<Song>) {
        val stats = child.childStats
        val points = mutableListOf<GraphPoint>()
        var summary = ""

        when (category) {
            "Music" -> {
                // Convert Map<String, Int> to GraphPoints
                val sorted = stats.musicPreferences.entries.sortedByDescending { it.value }.take(5)

                sorted.forEach { entry ->
                    val url = entry.key
                    val playCount = entry.value

                    // LOOKUP LOGIC: Find the song object with the matching URL
                    val matchingSong = allSongs.find { it.url == url }

                    // If found, use the name. If not (deleted song), use the filename from URL
                    val labelName = matchingSong?.name ?: url.substringAfterLast('/').substringBefore('?')

                    points.add(GraphPoint(labelName, playCount.toFloat(), 0xFFD561F2)) // Purple
                }
                summary = if(points.isNotEmpty()) "Most played: ${points.first().label}" else "No music data yet."
            }
            "Lights" -> {
                stats.colorPreferences.forEach { (color, count) ->
                    // Map color names to actual Hex colors for the bars
                    val barColor = when(color) {
                        "Red" -> 0xFFFF6B6B
                        "Yellow" -> 0xFFFFC758
                        "Green" -> 0xFF51CF66
                        "Blue" -> 0xFF239CA1
                        "Purple" -> 0xFFD561F2
                        else -> 0xFFCCCCCC
                    }
                    points.add(GraphPoint(color, count.toFloat(), barColor))
                }
                summary = "Color usage distribution."
            }
            "Time of Day" -> {
                // Ensure order: Morning -> Night
                val order = listOf("Morning", "Afternoon", "Evening", "Night")
                order.forEach { time ->
                    val count = stats.timeOfDayUsage[time] ?: 0
                    points.add(GraphPoint(time, count.toFloat(), 0xFFFFC758)) // Orange
                }
                summary = "Active stroller times."
            }
            "Overview" -> {
                // Overview isn't a graph, handled separately in UI usually,
                // but we can clear graph data to signal UI to show cards
                points.clear()
                summary = "Total Distance: ${"%.2f".format(stats.totalDistanceTravelled)}m\nTotal Time: ${stats.totalTimeInStrollerMinutes} mins"
            }
        }

        _graphData.value = points
        _summaryText.value = summary
    }
}