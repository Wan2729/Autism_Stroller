package com.example.autismstroller.reusables

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.autismstroller.R
import com.example.autismstroller.utilities.AppColors

@Composable
fun HeaderFragment(
    profileBitmap: Bitmap,
    username: String = "Header",
    level: Int = 1,
    progress: Float = 0.0f,
    profileOnClick: () -> Unit = {}
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.SoftPink)
            .size(60.dp), // Ensure it has a defined height
        contentAlignment = Alignment.Center // Aligns content in the center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.width(16.dp))
            Image(
                bitmap = profileBitmap.asImageBitmap(),
                contentDescription = "Profile",
                modifier = Modifier
                    .size(40.dp) // Profile picture size
                    .clip(CircleShape) // Makes it circular
                    .clickable { profileOnClick() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(username, color = Color(0xFF000000))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                ) {
                    LinearProgressIndicator(
                        progress = progress, // Experience progress
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(15.dp)
                            .clip(RoundedCornerShape(100.dp)), // Rounded edges for the bar
                        color = AppColors.Yellow
                    )
                    Text(
                        text = "Level $level",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}