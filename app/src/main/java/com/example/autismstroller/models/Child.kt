package com.example.autismstroller.models

data class Child(
    val id: String = "",
    val parentId: String = "",
    val profilePicture: String = "",
    val name: String = "",
    val age: Int = 0,
    val gender: String = "Male",
    val notes: String = "",

    val childStats: ChildStats = ChildStats()
)

data class ChildStats(
    val musicPreferences: Map<String, Int> = emptyMap(),
    val colorPreferences: Map<String, Int> = emptyMap(),
    val brightnessSum: Long = 0,
    val brightnessUsageCount: Int = 0,
    val totalDistanceTravelled: Double = 0.0, // Cumulative
    val coExposureSum: Double = 0.0,
    val tempExposureSum: Double = 0.0,
    val sensorReadingCount: Int = 0,
    val totalTimeInStrollerMinutes: Long = 0,
    val timeOfDayUsage: Map<String, Int> = emptyMap()
)
