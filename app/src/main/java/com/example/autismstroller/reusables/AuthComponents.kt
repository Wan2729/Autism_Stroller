package com.example.autismstroller.reusables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autismstroller.utilities.AppColors

// 1. A shared background that matches your Dashboard vibe
@Composable
fun ThemedBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppColors.SoftPink,
                        AppColors.SoftBlue.copy(alpha = 0.6f),
                        Color.White
                    )
                )
            )
    ) {
        // Decorative Circles (Optional, adds depth)
        Box(modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 50.dp, y = (-50).dp)
            .size(200.dp)
            .clip(CircleShape)
            .background(AppColors.SoftYellow.copy(alpha = 0.4f)))

        Box(modifier = Modifier
            .align(Alignment.BottomStart)
            .offset(x = (-50).dp, y = 50.dp)
            .size(250.dp)
            .clip(CircleShape)
            .background(AppColors.SoftBlue.copy(alpha = 0.4f)))

        content()
    }
}

// 2. Beautiful Rounded Text Field
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 14.sp) },
        leadingIcon = if (icon != null) {
            { Icon(icon, contentDescription = null, tint = AppColors.SoftPink) }
        } else null,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.AddCircle else Icons.Default.CheckCircle,
                        contentDescription = "Toggle Password"
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        singleLine = true,
        shape = RoundedCornerShape(50), // Fully rounded
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = AppColors.SoftPink,
            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
            containerColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// 3. Selection Chip for Gender
@Composable
fun GenderSelectionRow(selectedGender: String, onGenderSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("Male", "Female").forEach { gender ->
            val isSelected = selectedGender == gender
            val color = if (gender == "Male") AppColors.SoftBlue else AppColors.SoftPink

            Surface(
                color = if (isSelected) color else Color.White,
                border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, color) else null,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .height(45.dp)
                    .clickable { onGenderSelected(gender) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = gender,
                        color = if (isSelected) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}