package com.example.autismstroller.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.autismstroller.R
import com.example.autismstroller.functional.AchievementHandler
import com.example.autismstroller.functional.AchievementHandlerFactory
import com.example.autismstroller.functional.BLEControllerHandler
import com.example.autismstroller.functional.BLEHandler
import com.example.autismstroller.functional.BLEListenerHandler
import com.example.autismstroller.functional.ChildHandler
import com.example.autismstroller.functional.MusicPlayer
import com.example.autismstroller.functional.NOTIFY_UUID
import com.example.autismstroller.functional.SERVICE_UUID
import com.example.autismstroller.functional.SongHandler
import com.example.autismstroller.functional.UserHandler
import com.example.autismstroller.functional.WRITE_UUID
import com.example.autismstroller.functional.supabase
import com.example.autismstroller.reusables.ChildSelectionDialog
import com.example.autismstroller.reusables.CircularButton
import com.example.autismstroller.reusables.CircularGlowRing
import com.example.autismstroller.reusables.DisplayAnimationDialog
import com.example.autismstroller.reusables.LightingSettingDialog
import com.example.autismstroller.reusables.DistanceCOTempFragment
import com.example.autismstroller.reusables.FindMyStrollerDialog
import com.example.autismstroller.reusables.SmallWidthButton
import com.example.autismstroller.reusables.SongPickerDialog
import com.example.autismstroller.reusables.StrollerEmotion
import com.example.autismstroller.reusables.VideoStreamDialog
import com.example.autismstroller.reusables.WiFiConnectDialog
import com.example.autismstroller.utilities.AppColors
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ManageStrollerScreen(navController: NavHostController, bleHandler: BLEHandler){
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid?:"INVALID"
    val scope = rememberCoroutineScope()

    // -- Event Handler
    val userHandler: UserHandler = viewModel()
    val achievementHandler: AchievementHandler = viewModel(
        factory = AchievementHandlerFactory(userHandler)
    )

    // -- Value section for BLE connection handler
    val serviceUUID = SERVICE_UUID
    val writeUUID = WRITE_UUID
    val notifyUUID = NOTIFY_UUID
    val espConnected by bleHandler.espConnected.collectAsState()
    val bleControllerHandler = remember {
        BLEControllerHandler(
            gattProvider = { bleHandler.getGatt() },
            characteristicUUID = UUID.fromString(writeUUID),
            serviceUUID = UUID.fromString(serviceUUID)
        )
    }
    val bleListenerHandler = remember {
        BLEListenerHandler(
            gattProvider = { bleHandler.getGatt() },
            notifyCharacteristicUUID = UUID.fromString(notifyUUID),
            serviceUUID = UUID.fromString(serviceUUID)
        )
    }
    LaunchedEffect(espConnected) {
        if (espConnected) {
            // Important: We must enable notifications once connected
            bleHandler.setListener(bleListenerHandler)
            // Small delay to ensure services are discovered
            kotlinx.coroutines.delay(500)
            bleListenerHandler.enableNotifications()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            bleHandler.setListener(null)
        }
    }

    // -- Variable section for Display
    var isDisplayOn by remember { mutableStateOf(false) }
    var currentDisplayAnimation by remember { mutableStateOf(1) } // Default 1
    var showDisplaySetting by remember { mutableStateOf(false) }

    // -- Variable section for LED dialog management
    var currentLightColor by remember { mutableStateOf("128_250_250") }
    var showLightingSetting by remember { mutableStateOf(false) }
    var currentAnimation by remember{ mutableStateOf(1) }
    var isLightOn by remember { mutableStateOf(false) }

    // -- Variable section for Music management
    val songHandler: SongHandler = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // REPLACE 'YourSupabaseObject.client' with your actual client instance
                return SongHandler(supabase) as T
            }
        }
    )
    var selectedSongUrl by remember {
        mutableStateOf("https://bdqlurylipbhnapdhgky.supabase.co/storage/v1/object/public/music/surahannas.mp3")
    }
    val songsList by songHandler.songs.collectAsState()
    var showSongPicker by remember { mutableStateOf(false) }
    val musicPlayer = remember { MusicPlayer(context) }
    val songIsPlaying by musicPlayer.isPlaying.collectAsState()

    // -- Variable for listener
    val sensorData by bleListenerHandler.sensorData.collectAsState()
    val tempValue = sensorData.temp?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull() ?: 0.0
    val coValue = sensorData.gas?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull() ?: 0.0
    val animationRes = remember(espConnected, tempValue, coValue) {
        when {
            // ALERT: Temp >= 40 OR CO >= 2000
            tempValue >= 40.0 || coValue >= 2000.0 -> StrollerEmotion.ALERT // Make sure you have this JSON
            // MOVING: Connected but safe
            espConnected -> StrollerEmotion.MOVING // Make sure you have this JSON
            // IDLE: Not connected
            else -> StrollerEmotion.IDLE // Make sure you have this JSON
        }
    }

    // -- Variable section for Find my stroller
    val findingAlertSound = "https://bdqlurylipbhnapdhgky.supabase.co/storage/v1/object/public/music/findmydevice.mp3"
    var isFinding by remember { mutableStateOf(false) }
    var showFindMyStrollerDialog by remember { mutableStateOf(false) }
    val gpsLocation by bleListenerHandler.gpsLocation.collectAsState()

    // -- Variable for Assign child
    var assignedChildName by remember { mutableStateOf<String?>(null) }
    var assignedChildId by remember { mutableStateOf<String?>(null) }
    val childHandler: ChildHandler = viewModel()
    val childrenList by childHandler.childrenList.collectAsState()
    var showListOfChildren by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        childHandler.fetchChildren()
    }
    var sessionStartTime by remember { mutableStateOf(0L) }
    var sessionStartDistance by remember { mutableStateOf(0.0) }

    // -- Variable for Camera
    var showCameraDialog by remember { mutableStateOf(false) }
    val cameraIP by bleListenerHandler.cameraIP.collectAsState()
    var showVideoPlayer by remember { mutableStateOf(false) }
    LaunchedEffect(cameraIP) {
        if (cameraIP != null) {
            // Valid IP received! Close the WiFi dialog and open Video
            showCameraDialog = false
            showVideoPlayer = true
            Toast.makeText(context, "Camera Online: $cameraIP", Toast.LENGTH_LONG).show()
        }
    }

    // -- UI Start
    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ){
        Text(text = "Manage Stroller", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(0.5f))
        Row (Modifier.weight(2f)){
            Spacer(modifier = Modifier.weight(0.2f))
            Column (
                Modifier
                    .weight(8f)
                    .clip(RoundedCornerShape(20.dp))
            ){
                DistanceCOTempFragment(
                    distance = sensorData.dist ?:"",
                    co = sensorData.gas ?: "",
                    temp = sensorData.temp?: "",
                    currentEmotion = animationRes
//                    onClick = { navController.navigate("bluetoothListScreen") }
                )
            }
            Spacer(modifier = Modifier.weight(0.2f))
        }
        Column(
            Modifier
                .weight(3f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // -- Group 1 : Display
                Column {
                    Box(contentAlignment = Alignment.Center) {
                        // Glow ring triggers when ESP is connected AND display is logically ON
                        CircularGlowRing(
                            isActive = espConnected && isDisplayOn,
                            size = 56.dp,
                            color = AppColors.SoftPink
                        )

                        CircularButton(
                            iconRes = R.drawable.icon_square,
                            buttonColor = AppColors.SoftPink,
                            enabled = espConnected, // Only enable if connected
                            onClick = {
                                if(espConnected){
                                    isDisplayOn = !isDisplayOn

                                    if(isDisplayOn) {
                                        // Send the currently selected animation (1 or 2)
                                        bleControllerHandler.setDisplayMode(currentDisplayAnimation)
                                        Toast.makeText(context, "Display ON: Mode $currentDisplayAnimation", Toast.LENGTH_SHORT).show()
                                    } else {
                                        // Send -1 to close
                                        bleControllerHandler.setDisplayMode(-1)
                                        Toast.makeText(context, "Display OFF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                    SmallWidthButton(
                        textColor = Color.Black,
                        gradient = Brush.sweepGradient(colors = listOf(AppColors.SoftPink, AppColors.White))
                    ){
                        Log.d("Customize", "Customize Display clicked")
                        showDisplaySetting = true
                    }
                }
                // -- Group 2 : Speaker
                Column {
                    Box(contentAlignment = Alignment.Center) {
                        CircularGlowRing(isActive = espConnected && songIsPlaying && !isFinding, size = 56.dp, color = AppColors.SoftBlue)

                        CircularButton(
                            iconRes = R.drawable.icon_sound,
                            buttonColor = AppColors.SoftBlue,
                            enabled = espConnected,
                            onClick = {
                                if (!songIsPlaying && !isFinding) {
                                    if (selectedSongUrl.isNotEmpty()) {
                                        Log.d("Play Sound", "Playing: $selectedSongUrl")
                                        musicPlayer.playSong(selectedSongUrl)

                                        // -- Record Listen Count (Firestore) --
                                        val currentSong = songsList.find { it.url == selectedSongUrl }
                                        if (currentSong != null) {
                                            songHandler.recordListen(currentSong)
                                        }

                                        // -- Child stat update --
                                        if (assignedChildId != null) {
                                            childHandler.recordMusicPlay(assignedChildId!!, selectedSongUrl)
                                        }
                                    } else {
                                        Toast.makeText(context, "Please select a song first", Toast.LENGTH_SHORT).show()
                                        showSongPicker = true
                                    }
                                } else {
                                    musicPlayer.stop()
                                }
                            }
                        )
                    }
                    SmallWidthButton(
                        textColor = Color.Black,
                        gradient = Brush.sweepGradient(colors = listOf(AppColors.SoftBlue, AppColors.White))
                    ) {
                        musicPlayer.stop();
                        showSongPicker = true
                    }
                }
                // -- Group 3 : Light
                Column {
                    Box(contentAlignment = Alignment.Center){
                        CircularGlowRing(isActive = espConnected && isLightOn, size = 56.dp, color = AppColors.SoftYellow)

                        CircularButton(
                            iconRes = R.drawable.icon_light,
                            buttonColor = AppColors.SoftYellow,
                            enabled = espConnected,
                            onClick = {
                                if (espConnected) {
                                    isLightOn = !isLightOn
                                    bleControllerHandler.toggleLED(currentLightColor, isLightOn, currentAnimation)
                                    if(isLightOn) {
                                        Toast.makeText(context, "Turn ON LED", Toast.LENGTH_SHORT).show()
                                        scope.launch {
                                            val stat = "turnOnLEDCount"
                                            userHandler.increment(uid, stat, 1)
                                            if(userHandler.getStatValue(uid, stat) == 1){
                                                val newAchievements = achievementHandler.checkAndUnlockByStat(user?.uid ?: "INVALID",   stat)
                                                if(newAchievements.isNotEmpty()){
                                                    Toast.makeText(context, "New Achievement Unlocked !", Toast.LENGTH_SHORT).show()
                                                }
                                            }

                                            // -- Child stat updare
                                            if (assignedChildId != null) {
                                                childHandler.recordLightUsage(assignedChildId!!, currentLightColor)
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                    SmallWidthButton(textColor = Color.Black, gradient = Brush.sweepGradient(colors = listOf(AppColors.SoftYellow, AppColors.White))){
                        Log.d("Customize", "Customize Light clicked")
                        showLightingSetting = true
                        if (espConnected && isLightOn) {
                            bleControllerHandler.toggleLED(currentLightColor, isLightOn, currentAnimation)
                            isLightOn = !isLightOn
                        }
                    }
                }
            }
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // -- Group 4 : Find my stroller
                Column {
                    Box(contentAlignment = Alignment.Center) {
                        CircularGlowRing(
                            isActive = espConnected && isFinding, size = 56.dp, color = AppColors.SoftBlue
                        )

                        CircularButton(
                            iconRes = R.drawable.icon_location,
                            buttonColor = AppColors.SoftBlue,
                            enabled = espConnected,
                            onClick = {
                                if (espConnected) {
                                    // CHANGED LOGIC: Open Dialog instead of toggle immediately
                                    showFindMyStrollerDialog = true
                                } else {
                                    Toast.makeText(context, "Connect to stroller first", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
                
                // -- Group 5 : Assign Child
                Column {
                    Box(contentAlignment = Alignment.Center) {
                        // Glows if a child is currently assigned
                        CircularGlowRing(
                            isActive = assignedChildName != null,
                            size = 56.dp,
                            color = AppColors.SoftYellow
                        )

                        CircularButton(
                            iconRes = R.drawable.icon_stroller,
                            buttonColor = AppColors.SoftYellow,
                            enabled = espConnected,
                            onClick = {
                                // Open the child selection dialog
                                showListOfChildren = true
                            }
                        )
                    }
                }

                // -- Group 6 : Camera
                CircularButton(iconRes = R.drawable.icon_video, buttonColor = AppColors.SoftPink, enabled = espConnected) {
                    if(espConnected) {
                        showCameraDialog = true
                    } else {
                        Toast.makeText(context, "Connect to Stroller first", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            if (assignedChildName != null) {
                // Active Status Pill
                androidx.compose.material3.Surface(
                    color = AppColors.SoftBlue,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { showListOfChildren = true }
                ) {
                    Text(
                        text = "Assigned to: $assignedChildName",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            } else {
                // Idle Status Text
                Text(
                    text = "No Child Assigned",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { showListOfChildren = true }
                )
            }
        }

        // -- Dialog Handler section
        if (showDisplaySetting) {
            DisplayAnimationDialog(
                onDismiss = { showDisplaySetting = false },
                onAnimationSelected = { animationMode ->
                    currentDisplayAnimation = animationMode
                    showDisplaySetting = false

                    // If display is currently ON, update it immediately
                    if (espConnected && isDisplayOn) {
                        bleControllerHandler.setDisplayMode(animationMode)
                    }
                    Toast.makeText(context, "Style $animationMode Selected", Toast.LENGTH_SHORT).show()
                }
            )
        }
        if (showLightingSetting) {
            LightingSettingDialog(
                initialColorString = currentLightColor,
                onColorSelected = { newColor ->
                    // Update the state with the new color when "Apply" is clicked
                    currentLightColor = newColor
                    // In a real app, you would send this 'newColor' to the BLE handler here
                    Log.d("ColorPicker", "New color applied: $newColor")
                },
                onAnimationSelected = { newAnimation ->
                    currentAnimation = newAnimation
                    Log.d("AnimationChosen", "Animation applied: $newAnimation")
                },
                onDismiss = {
                    showLightingSetting = false
                }
            )
        }
        if (showSongPicker) {
            SongPickerDialog(
                songs = songsList,
                onDismiss = { showSongPicker = false },
                onSongSelected = { song ->
                    selectedSongUrl = song.url
                    showSongPicker = false

                    // Optional: Toast to confirm selection
                    Toast.makeText(context, "Selected: ${song.name}", Toast.LENGTH_SHORT).show()
                },
                onUploadRequest = { name, uri, isPublic ->
                    songHandler.uploadSong(context, name, uri, isPublic)
                }
            )
        }
        if (showFindMyStrollerDialog) {
            FindMyStrollerDialog(
                location = gpsLocation,
                isSoundPlaying = isFinding,
                onToggleSound = {
                    if (isFinding) {
                        // Stop Sound
                        musicPlayer.stop()
                        isFinding = false
                    } else {
                        // Play Alert
                        musicPlayer.stop() // Stop any song playing
                        musicPlayer.playSong(findingAlertSound, isLooping = true)
                        isFinding = true
                    }
                },
                onDismiss = {
                    showFindMyStrollerDialog = false
                    // Optional: If you want sound to stop when they close the map:
                    // musicPlayer.stop()
                    // isFinding = false
                }
            )
        }
        if (showListOfChildren) {
            ChildSelectionDialog(
                children = childrenList,
                onDismiss = { showListOfChildren = false },
                onChildSelected = { child ->
                    if (child != null) {
                        assignedChildName = child.name
                        assignedChildId = child.id

                        sessionStartTime = System.currentTimeMillis()
                        sessionStartDistance = sensorData.dist?.toDoubleOrNull() ?: 0.0

                        Toast.makeText(context, "Assigned to ${child.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        // UNASSIGN LOGIC -> SAVE STATS
                        if (assignedChildId != null) {
                            val endTime = System.currentTimeMillis()
                            val durationMin = (endTime - sessionStartTime) / 60000

                            // Calculate distance
                            val currentDist = sensorData.dist?.toDoubleOrNull() ?: 0.0
                            // Ensure we don't get negative distance if sessionStart was higher (e.g. restart)
                            val distanceTravelled = if (currentDist > sessionStartDistance) currentDist - sessionStartDistance else 0.0

                            // Get Sensor Data
                            val currentCo = sensorData.gas?.toDoubleOrNull() ?: 0.0
                            val currentTemp = sensorData.temp?.toDoubleOrNull() ?: 0.0

                            // LOGGING TO DEBUG
                            Log.d("Unassign", "Saving: Dist=$distanceTravelled, CO=$currentCo, Temp=$currentTemp")

                            childHandler.recordSensorSession(
                                childId = assignedChildId!!,
                                distanceAdded = distanceTravelled,
                                avgCo = currentCo,
                                avgTemp = currentTemp,
                                durationMinutes = durationMin
                            )

                            childHandler.recordTimeOfDay(assignedChildId!!, sessionStartTime)
                            Toast.makeText(context, "Session saved for ${assignedChildName}", Toast.LENGTH_SHORT).show()
                        }
                        // Unassign case
                        assignedChildName = null
                        assignedChildId = null
                        Toast.makeText(context, "Child Unassigned", Toast.LENGTH_SHORT).show()
                    }
                    showListOfChildren = false
                }
            )
        }
        if (showCameraDialog) {
            WiFiConnectDialog(
                onDismiss = { showCameraDialog = false },
                onConnect = { ssid, password ->
                    Log.d("Camera", "Sending credentials: $ssid")
                    bleControllerHandler.sendWiFiCredentials(ssid, password)
                    showCameraDialog = false
                    Toast.makeText(context, "Credentials sent to Stroller...", Toast.LENGTH_LONG).show()
                }
            )
        }
        if (showVideoPlayer && cameraIP != null) {
            VideoStreamDialog(
                ipAddress = cameraIP!!,
                onDismiss = {
                    showVideoPlayer = false
                    // Optional: Clear the IP so it doesn't reopen immediately if you reconnect
                     bleListenerHandler.clearIP()
                }
            )
        }

        Spacer(Modifier.weight(2f))
    }
}