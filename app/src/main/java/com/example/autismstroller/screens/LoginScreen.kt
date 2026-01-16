package com.example.autismstroller.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.autismstroller.functional.AuthHandler
import com.example.autismstroller.functional.AuthState
import com.example.autismstroller.reusables.AuthTextField
import com.example.autismstroller.reusables.ThemedBackground
import com.example.autismstroller.utilities.AppColors

@Composable
fun LoginScreen(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val accountHandler: AuthHandler = viewModel()
    val myAuthState by accountHandler.authstate.collectAsState()
    val context = LocalContext.current

    // Auth Logic
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
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Text("Welcome Back!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Text("Login to continue", fontSize = 16.sp, color = Color.Gray)

            Spacer(Modifier.height(32.dp))

            // Form Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AuthTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        icon = Icons.Default.Email
                    )

                    AuthTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        icon = Icons.Default.Lock,
                        isPassword = true
                    )

                    // Login Button
                    Button(
                        onClick = { accountHandler.login(email, password) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.SoftBlue),
                        shape = RoundedCornerShape(50),
                        enabled = myAuthState != AuthState.Loading
                    ) {
                        if (myAuthState == AuthState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("LOG IN", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Footer Links
            Row {
                Text("Don't have an account? ", color = Color.Gray)
                Text(
                    "Register",
                    color = AppColors.SoftPink,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { navController.navigate("registerScreen") }
                )
            }
        }
    }
}