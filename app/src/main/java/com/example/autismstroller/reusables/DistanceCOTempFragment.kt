package com.example.autismstroller.reusables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.autismstroller.R
import com.example.autismstroller.utilities.AppColors

@Composable
fun DistanceCOTempFragment(
    distance: String = "1000",
    co: String = "12",
    temp: String = "30",
    currentEmotion: StrollerEmotion = StrollerEmotion.IDLE,
    onClick: () -> Unit = {}
) {
    val animRes = when (currentEmotion) {
        StrollerEmotion.IDLE -> R.raw.avatar_idle
        StrollerEmotion.MOVING -> R.raw.avatar_moving
        StrollerEmotion.ALERT -> R.raw.avatar_alert
        StrollerEmotion.HAPPY -> TODO()
    }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animRes))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = true
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp) // Fixed height to keep the card shape consistent
            .clip(RoundedCornerShape(24.dp)) // Smoother corners
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFD561F2),
                        Color(0xFFE9AFF8)
                    ),
                    start = Offset.Zero,
                    end = Offset(700f, 500f)
                )
            )
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp), // Add padding inside the card
        contentAlignment = Alignment.CenterStart
    ){
        Box(modifier = Modifier.matchParentSize()) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .matchParentSize() // Fills the Box completely
                    .alpha(0.5f),      // Adjust opacity so text is readable
                contentScale = ContentScale.Crop // Zooms in to fill bounds
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // --- LEFT SIDE: Big Distance Text ---
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Distance",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                // Row to align the number and unit ("m") nicely
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = distance,
                        color = Color.White,
                        fontSize = 56.sp, // Much Bigger
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 56.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "m",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp) // Align to baseline
                    )
                }
            }

            // --- RIGHT SIDE: Sensors (Stacked vertically) ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly, // Spread them out vertically
                modifier = Modifier.fillMaxHeight()
            ) {
                // CO Section
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularButton(iconRes = R.drawable.icon_co, buttonColor = AppColors.Blue, size = 40.dp) {}
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${co} PPM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Temp Section
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularButton(iconRes = R.drawable.icon_temperature, buttonColor = AppColors.Yellow, size = 40.dp) {}
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${temp}°C", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

enum class StrollerEmotion {
    IDLE,       // Normal state (Your current JSON)
    MOVING,     // When distance is increasing
    ALERT,      // When CO/Temp is high
    HAPPY       // When music is playing
}