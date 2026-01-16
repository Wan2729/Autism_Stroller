package com.example.autismstroller.reusables

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.autismstroller.R
import com.example.autismstroller.utilities.AppColors

@Composable
fun CircularButton(
    iconRes: Int,
    buttonColor: Color,
    size: Dp = 56.dp,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(size) // Adjust size as needed
            .clip(CircleShape)
            .background(if (enabled) buttonColor else Color.Gray) // Change color as needed
            .clickable { onClick() }, // Clickable action
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes), // Use your SVG resource
            contentDescription = "Circular Button",
            tint = Color.Black, // Adjust icon color
            modifier = Modifier.size(size/2) // Adjust icon size
        )
    }
}

@Composable
fun CircularGlowRing(
    isActive: Boolean,
    size: Dp,
    strokeWidth: Dp = 4.dp,
    color: Color = Color.Yellow
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring")

    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "angle"
    )

    if (isActive) {
        Canvas(
            modifier = Modifier.size(size + 10.dp)
        ) {
            drawArc(
                color = color,
                startAngle = angle,
                sweepAngle = 90f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}