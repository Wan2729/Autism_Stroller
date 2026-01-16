package com.example.autismstroller.utilities

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── HELPER FOR SHADOWS ──
// Helps keep the code clean
fun Modifier.cardShadow(): Modifier = this
    .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
    .background(Color.White, shape = RoundedCornerShape(12.dp))
    .clip(RoundedCornerShape(12.dp))