package com.example.autismstroller.functional

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autismstroller.models.Child
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChildHandler(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val collectionRef = db.collection("children")

    private val _childrenList = MutableStateFlow<List<Child>>(emptyList())
    val childrenList: StateFlow<List<Child>> = _childrenList

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun fetchChildren() {
        val parentId = auth.currentUser?.uid ?: return

        _isLoading.value = true
        collectionRef.whereEqualTo("parentId", parentId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val children = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Child::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _childrenList.value = children
                }
                _isLoading.value = false
            }
    }

    fun getChildById(childId: String, onSuccess: (Child?) -> Unit) {
        if (childId == "new") {
            onSuccess(null)
            return
        }
        viewModelScope.launch {
            try {
                val document = collectionRef.document(childId).get().await()
                if (document.exists()) {
                    onSuccess(document.toObject(Child::class.java))
                } else {
                    onSuccess(null)
                }
            } catch (e: Exception) {
                onSuccess(null)
            }
        }
    }

    /**
     * Save Child (Create or Update).
     * The profilePicture string in the child object must already be the Base64 string.
     */
    fun saveChild(
        child: Child,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val parentId = auth.currentUser?.uid
        if (parentId == null) {
            onError("User not logged in")
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                // 1. Determine ID
                val isNew = child.id.isEmpty() || child.id == "new"
                val finalId = if (isNew) UUID.randomUUID().toString() else child.id

                // 2. Prepare Object (Inject ParentID and Ensure ID is set)
                val childToSave = child.copy(
                    id = finalId,
                    parentId = parentId
                    // profilePicture is already in 'child' as Base64
                )

                // 3. Save to Firestore
                collectionRef.document(finalId).set(childToSave).await()

                _isLoading.value = false
                onSuccess()

            } catch (e: Exception) {
                _isLoading.value = false
                onError(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun deleteChild(childId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                collectionRef.document(childId).delete().await()
                _isLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                onError(e.message ?: "Failed to delete")
            }
        }
    }

    /**
     * 1. Record Music
     * Increments the count for a specific song URL.
     */
    fun recordMusicPlay(childId: String, songUrl: String) {
        val ref = collectionRef.document(childId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val currentStats = snapshot.get("childStats") as? Map<String, Any> ?: emptyMap()

            // Get current music map
            val musicMap = (currentStats["musicPreferences"] as? Map<String, Long>)?.toMutableMap() ?: mutableMapOf()

            // Increment count
            val currentCount = musicMap[songUrl] ?: 0L
            musicMap[songUrl] = currentCount + 1

            // Write back
            transaction.update(ref, "childStats.musicPreferences", musicMap)
        }
    }

    /**
     * 2. Record Light Usage
     * Takes raw HSV string (e.g., "120_100_100") and categorizes it.
     */
    fun recordLightUsage(childId: String, hsvString: String) {
        val parts = hsvString.split("_")
        if (parts.size != 3) return

        val hue = parts[0].toIntOrNull() ?: 0
        val brightness = parts[2].toIntOrNull() ?: 0

        if (brightness == 0) return // Don't record if light is off/black

        // Categorize Hue
        val colorCategory = when (hue) {
            in 0..30 -> "Red"
            in 31..90 -> "Yellow"
            in 91..150 -> "Green"
            in 151..210 -> "Cyan"
            in 211..270 -> "Blue"
            in 271..330 -> "Purple"
            else -> "Red" // 331-360
        }

        val ref = collectionRef.document(childId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)

            // 1. Update Color Count
            val colorMap = (snapshot.get("childStats.colorPreferences") as? Map<String, Long>)?.toMutableMap() ?: mutableMapOf()
            val count = colorMap[colorCategory] ?: 0L
            colorMap[colorCategory] = count + 1

            // 2. Update Brightness Average
            val currentSum = snapshot.getLong("childStats.brightnessSum") ?: 0L
            val currentCount = snapshot.getLong("childStats.brightnessUsageCount") ?: 0L

            transaction.update(ref, mapOf(
                "childStats.colorPreferences" to colorMap,
                "childStats.brightnessSum" to currentSum + brightness,
                "childStats.brightnessUsageCount" to currentCount + 1
            ))
        }
    }

    /**
     * 3. Record Sensor Data
     * Adds to the accumulators. Call this periodically (e.g., every 5 mins or when unassigning).
     */
    fun recordSensorSession(childId: String, distanceAdded: Double, avgCo: Double, avgTemp: Double, durationMinutes: Long) {
        val ref = collectionRef.document(childId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)

            // 1. Always update Distance and Time
            // Ensure we handle nulls gracefully by defaulting to 0.0/0L if field missing
            val currentDist = snapshot.get("childStats.totalDistanceTravelled")?.toString()?.toDoubleOrNull() ?: 0.0
            val currentTime = snapshot.get("childStats.totalTimeInStrollerMinutes")?.toString()?.toLongOrNull() ?: 0L

            val totalDist = currentDist + distanceAdded
            val time = currentTime + durationMinutes

            val updates = mutableMapOf<String, Any>(
                "childStats.totalDistanceTravelled" to totalDist,
                "childStats.totalTimeInStrollerMinutes" to time
            )

            // 2. FIXED LOGIC HERE
            // We allow CO to be 0.0 (Clean air), but we usually want Temp > 0.0 (unless it's freezing, but usually 0.0 means sensor error in this context)
            // OR simply remove the check if you trust the sensor data passed in.

            val isValidReading = avgTemp > 0.0 // Only check Temp. Allow CO to be 0.

            if (isValidReading) {
                val currentCoSum = snapshot.get("childStats.coExposureSum")?.toString()?.toDoubleOrNull() ?: 0.0
                val currentTempSum = snapshot.get("childStats.tempExposureSum")?.toString()?.toDoubleOrNull() ?: 0.0
                val currentCount = snapshot.get("childStats.sensorReadingCount")?.toString()?.toLongOrNull() ?: 0L

                updates["childStats.coExposureSum"] = currentCoSum + avgCo
                updates["childStats.tempExposureSum"] = currentTempSum + avgTemp
                updates["childStats.sensorReadingCount"] = currentCount + 1
            }

            transaction.update(ref, updates)
        }
    }

    fun recordTimeOfDay(childId: String, startTimeMillis: Long) {
        // 1. Determine Time Category
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = startTimeMillis
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)

        val timeCategory = when (hour) {
            in 6..11 -> "Morning"   // 6 AM - 11:59 AM
            in 12..16 -> "Afternoon" // 12 PM - 4:59 PM
            in 17..20 -> "Evening"   // 5 PM - 8:59 PM
            else -> "Night"          // 9 PM - 5:59 AM
        }

        // 2. Update Firestore
        val ref = collectionRef.document(childId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val currentStats = snapshot.get("childStats") as? Map<String, Any> ?: emptyMap()

            // Get current map or create empty one
            val timeMap = (currentStats["timeOfDayUsage"] as? Map<String, Long>)?.toMutableMap() ?: mutableMapOf()

            // Increment count for this category
            val currentCount = timeMap[timeCategory] ?: 0L
            timeMap[timeCategory] = currentCount + 1

            transaction.update(ref, "childStats.timeOfDayUsage", timeMap)
        }
    }
}