package com.example.autismstroller.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.autismstroller.R
import com.example.autismstroller.functional.AchievementHandler
import com.example.autismstroller.functional.AchievementHandlerFactory
import com.example.autismstroller.functional.UserHandler
import com.example.autismstroller.models.Achievement
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.example.autismstroller.reusables.AchievementSettingDialog
import com.example.autismstroller.utilities.AppColors
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AchievementListScreen(navController: NavController) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid?:"INVALID"
    var loading by remember { mutableStateOf(true) }
    val userHandler: UserHandler = viewModel()
    val achievementHandler: AchievementHandler = viewModel(
        factory = AchievementHandlerFactory(userHandler)
    )
    var showAchievementSetting by remember { mutableStateOf(false) }
    var achievements by remember { mutableStateOf<List<Achievement>>(emptyList()) }
    var achievementsDone by remember { mutableStateOf<List<Achievement>>(emptyList()) }
    var remainingAchievements by remember { mutableStateOf<List<Achievement>>(emptyList()) }

    LaunchedEffect(Unit){
        try {
            achievements = achievementHandler.getAllAchievement()
            achievementsDone = achievementHandler.getAchievementByUser(uid)
            val doneIds = achievementsDone.map { it.id }.toSet()
            remainingAchievements = achievements.filter { it.id !in doneIds }
        } finally {
            loading = false
        }
    }

    val achievementDummies = listOf(
        Achievement(
            id = "1",
            title = "First Walk",
            description = "Use stroller for the first time",
            completed = true
        ),
        Achievement(
            id = "2",
            title = "Soothing Sound",
            description = "Play music 5 times",
            completed = false
        ),
        Achievement(
            id = "3",
            title = "Bright Night",
            description = "Turn on LED light",
            completed = false
        )
    )
    val achievementsIcon = listOf(
        R.drawable.icon_stroller,
        R.drawable.icon_sound,
        R.drawable.icon_light

    )

    if(loading){ StartLoadingScreen() }
    else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // ── Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { navController.popBackStack() }
                )

                Spacer(Modifier.width(12.dp))

                Text(
                    text = "Achievements",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            // -- Button to add Achievement
            if(uid == "Zllp3uhWUySFomNGhMLnd5cbM4f2") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(AppColors.SoftYellow.toArgb()),
                                    Color(AppColors.SoftYellow.toArgb() + (0xFF239CA1 - 0xFF95DDE0))
                                )
                            )
                        )
                        .padding(16.dp)
                        .clickable { showAchievementSetting = true }
                ) {
                    Text(
                        text = "Create Achievement",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.White
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Optional Summary Card
            AchievementSummaryCard(
                unlockedCount = achievementsDone.count { it.completed },
                totalCount = achievements.size
            )

            Spacer(Modifier.height(20.dp))

            // ── Achievement List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(remainingAchievements) { achievement ->
                    AchievementCard(achievement)
                }
                items(achievementsDone) { achievement ->
                    AchievementCard(achievement)
                }
            }
        }
    }

    // -- Dialog section
    if(showAchievementSetting){
        AchievementSettingDialog(
            onDismiss = {
                showAchievementSetting = false
            },
            onAchievementCreated = { achievement ->
                achievementHandler.createAchievement(
                    achievement = achievement,
                    onSuccess = {
                        Toast
                            .makeText(context, "Achievement created", Toast.LENGTH_SHORT)
                            .show()
                        showAchievementSetting = false
                    },
                    onError = { e ->
                        Toast
                            .makeText(context, e.message ?: "Failed to create", Toast.LENGTH_LONG)
                            .show()
                    }
                )
            }
        )
    }
}

@Composable
fun AchievementSummaryCard(
    unlockedCount: Int,
    totalCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF95DDE0),
                        Color(0xFF239CA1)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "$unlockedCount / $totalCount completed",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun AchievementCard(
    achievement: Achievement
) {
    val backgroundBrush = if (achievement.completed) {
        Brush.linearGradient(
            // Greyish
            colors = listOf(
                Color(0xFFE0E0E0),
                Color(0xFFBDBDBD)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFE9AFF8),
                Color(0xFFD561F2)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundBrush)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // ── Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.icon_children),
                    contentDescription = achievement.title,
                    modifier = Modifier.size(32.dp),
                    tint = Color.Black
                )
            }

            Spacer(Modifier.width(16.dp))

            // ── Text + Progress
            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = achievement.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (achievement.completed) Color.Black else Color.DarkGray
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = achievement.description,
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )

                Spacer(Modifier.height(8.dp))

//                LinearProgressIndicator(
//                    progress = achievement.progress,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(6.dp)
//                        .clip(RoundedCornerShape(50)),
//                    color = if (achievement.completed) Color(0xFF6A1B9A) else Color.Gray,
//                    trackColor = Color.White.copy(alpha = 0.5f)
//                )
            }

            if (!achievement.completed) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.DarkGray
                )
            }
        }
    }
}


