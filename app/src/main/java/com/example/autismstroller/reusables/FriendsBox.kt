package com.example.autismstroller.reusables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autismstroller.utilities.cardShadow

@Composable
fun FriendsBox(
    count: Int,
    modifier: Modifier = Modifier
) {
    Box(
        // Applied custom shadow helper here
        modifier = modifier.cardShadow().padding(12.dp)
    ) {
        Column {
            Text(text = "Friends", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text(text = "$count", fontSize = 14.sp, color = Color.Black)
        }
    }
}