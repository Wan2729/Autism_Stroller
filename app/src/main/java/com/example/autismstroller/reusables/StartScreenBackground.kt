package com.example.autismstroller.reusables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.autismstroller.R
import com.example.autismstroller.utilities.AppColors

@Composable
fun StartScreenBackground(){
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(120.dp))
        Image(
            modifier = Modifier.size(280.dp),
            painter = painterResource(id = R.drawable.image_sun),
            contentDescription = "Sun"
        )
        Spacer(modifier = Modifier.height(60.dp))
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Image(
                modifier = Modifier.size(560.dp),
                painter = painterResource(id = R.drawable.image_mountain),
                contentDescription = "Image of mountain",
                contentScale = ContentScale.FillBounds
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Pink.copy(alpha = 0.5f))
    ){

    }

    // **Blue Gradient Overlay**
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter // Ensures the gradient sits at the bottom
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth() // Covers full width
                .fillMaxHeight(0.5f) // Limits height to 50% of the screen
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent, // Starts transparent at the top
                            Color.DarkGray.copy(alpha = 0.5f)  // Becomes solid blue at the bottom
                        )
                    )
                )
        )
    }
}

@Preview
@Composable
fun StartScreenBackgroundPreview() {
    StartScreenBackground()
}