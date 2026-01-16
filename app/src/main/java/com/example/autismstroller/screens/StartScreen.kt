package com.example.autismstroller.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.autismstroller.R
import com.example.autismstroller.functional.AuthHandler
import com.example.autismstroller.functional.AuthState
import com.example.autismstroller.reusables.ThemedBackground // Import new component
import com.example.autismstroller.utilities.AppColors

@Composable
fun StartScreen(navController: NavHostController) {
    val accountHandler: AuthHandler = viewModel()
    val myAuthState by accountHandler.authstate.collectAsState()

    LaunchedEffect(Unit) { accountHandler.checkAuthStatus() }

    LaunchedEffect(myAuthState) {
        if (myAuthState is AuthState.Authenticated) {
            navController.navigate("homeScreen") { popUpTo("startScreen") { inclusive = true } }
        }
    }

    ThemedBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // Logo Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Card(
                    shape = RoundedCornerShape(30.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.round_logo_sss_small),
                        contentDescription = "Logo",
                        modifier = Modifier.size(150.dp).padding(10.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Sensory Smart\nStroller",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    color = Color.Black.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Buttons Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { navController.navigate("loginScreen") },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.SoftBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { navController.navigate("registerScreen") },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.SoftPink),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Register", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}