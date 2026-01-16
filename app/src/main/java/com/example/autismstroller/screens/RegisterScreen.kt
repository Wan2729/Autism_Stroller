package com.example.autismstroller.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.autismstroller.R
import com.example.autismstroller.functional.AuthHandler
import com.example.autismstroller.functional.AuthState
import com.example.autismstroller.reusables.AuthTextField
import com.example.autismstroller.reusables.GenderSelectionRow
import com.example.autismstroller.reusables.ThemedBackground
import com.example.autismstroller.utilities.AppColors
import com.example.autismstroller.utilities.ImageManipulator

@Composable
fun RegisterScreen(navController: NavHostController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("Male") }

    // Avatar Logic
    val profileIcons = listOf(
        R.drawable.icon_defaultprofile1,
        R.drawable.icon_defaultprofile2,
        R.drawable.icon_defaultprofile3,
        R.drawable.icon_defaultprofile4
    )
    var currentAvatarRes by remember { mutableStateOf(profileIcons.first()) }

    val accountHandler: AuthHandler = viewModel()
    val myAuthState by accountHandler.authstate.collectAsState()
    val context = LocalContext.current

    // Auth Logic (Keep existing)
    when (myAuthState) {
        is AuthState.Authenticated -> {
            LaunchedEffect(Unit) {
                navController.navigate("homeScreen") {
                    popUpTo("loginScreen") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        is AuthState.Error -> {
            Toast.makeText(context, (myAuthState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
            accountHandler.clearError()
        }
        else -> Unit
    }

    ThemedBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()), // IMPT: Makes it scrollable
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Spacer(Modifier.height(20.dp))
            Text("Create Account", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)

            Spacer(Modifier.height(20.dp))

            // Avatar Picker (Visual)
            Box(contentAlignment = Alignment.BottomEnd) {
                Image(
                    painter = painterResource(id = currentAvatarRes),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            currentAvatarRes = profileIcons.random() // Randomize on click
                        }
                )
                // Small shuffle icon
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(AppColors.SoftPink),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            Text("Tap to shuffle avatar", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))

            Spacer(Modifier.height(24.dp))

            // Form Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AuthTextField(value = name, onValueChange = { name = it }, label = "Full Name", icon = Icons.Default.Person)
                    AuthTextField(value = email, onValueChange = { email = it }, label = "Email", icon = Icons.Default.Email)
                    AuthTextField(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Default.Lock, isPassword = true)
                    AuthTextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = "Phone Number", icon = Icons.Default.Phone, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        // Age Field (Small)
                        Box(modifier = Modifier.weight(0.4f)) {
                            AuthTextField(
                                value = age,
                                onValueChange = { if (it.all { char -> char.isDigit() }) age = it },
                                label = "Age",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        // Gender Selection (Chips)
                        Box(modifier = Modifier.weight(0.6f)) {
                            GenderSelectionRow(selectedGender) { selectedGender = it }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Register Button
                    Button(
                        onClick = {
                            val bitmap = ImageManipulator.vectorToBitmap(context, currentAvatarRes)
                            val imageString = ImageManipulator.encodeImageToBase64(bitmap)

                            accountHandler.register(
                                name = name, email = email, password = password,
                                phoneNumber = phoneNumber, age = age.toIntOrNull() ?: 0,
                                isMale = selectedGender == "Male", profilePhoto = imageString
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.SoftPink),
                        shape = RoundedCornerShape(50),
                        enabled = myAuthState != AuthState.Loading
                    ) {
                        if(myAuthState == AuthState.Loading) {
                            CircularProgressIndicator(color = Color.White)
                        } else {
                            Text("REGISTER", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row {
                Text("Already have an account? ", color = Color.Gray)
                Text(
                    "Login",
                    color = AppColors.SoftBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { navController.navigate("loginScreen") }
                )
            }
            Spacer(Modifier.height(40.dp)) // Extra space for scrolling
        }
    }
}