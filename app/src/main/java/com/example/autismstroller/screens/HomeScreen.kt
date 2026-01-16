package com.example.autismstroller.screens

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.autismstroller.R
import com.example.autismstroller.functional.AuthHandler
import com.example.autismstroller.functional.UserHandler
import com.example.autismstroller.functional.UserState
import com.example.autismstroller.models.User
import com.example.autismstroller.utilities.ImageManipulator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var loading by remember { mutableStateOf(true) }
    val uid = auth.uid?: "INVALID"

    var userDetails by remember {
        mutableStateOf<User?>(null)
    }
    val userHandler: UserHandler = viewModel()
    val userState by userHandler.userstate.collectAsState()
    val authHandler: AuthHandler = viewModel()

    val defaultPfpBitmap = ImageManipulator.vectorToBitmap(context, R.drawable.icon_loading_pfp)

    LaunchedEffect(auth){
        userHandler.getUserDetails(uid)
    }
    when (userState) {
        is UserState.UserDetails -> {
            userDetails = (userState as UserState.UserDetails).userDetail
            loading = false
        }
        is UserState.Error -> {
            Toast.makeText(context, "Error: ${(userState as UserState.Error).message}", Toast.LENGTH_SHORT).show()
        }
        else -> Unit
    }

    // -- UI Start
    if(loading){ StartLoadingScreen() }
    else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Profile Picture
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                val pfpBitmap = ImageManipulator.decodeBase64ToBitmap(userDetails?.profilePicture)
                    ?: defaultPfpBitmap

                Image(
                    modifier = Modifier
                        .size(56.dp)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .clickable { navController.navigate("myProfileScreen/${auth.uid}") },
                    painter = BitmapPainter(pfpBitmap.asImageBitmap()),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop
                )
            }

            // Welcome Text
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                val username = userDetails?.name ?: "Username"

                Column {
                    Text(
                        text = "Welcome back,",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = username,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }


            ManageStrollerCard(
                context,
                modifier = Modifier.clickable {
                    navController.navigate("manageStrollerScreen")
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(20.dp)
            ) {
                val cardModifier = Modifier
                    .fillMaxWidth(0.47f) // 40% of parent width
                    .aspectRatio(1f)    // keep square
                val childrenIcon =
                    ImageManipulator.vectorToBitmap(context, R.drawable.icon_children)
                val socialIcon = ImageManipulator.vectorToBitmap(context, R.drawable.icon_chat)
                val achievementIcon =
                    ImageManipulator.vectorToBitmap(context, R.drawable.icon_achievement)
                val analyticalIcon =
                    ImageManipulator.vectorToBitmap(context, R.drawable.icon_analytical)

                FeatureCard(
                    childrenIcon,
                    "Children",
                    modifier = cardModifier
                        .align(Alignment.TopStart)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFBDCE9),
                                    Color(0xFFD94583)
                                ),
                                start = Offset.Zero,
                                end = Offset(500f, 300f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { navController.navigate("childListScreen") }
                )
                FeatureCard(
                    socialIcon,
                    "Social",
                    modifier = cardModifier
                        .align(Alignment.TopEnd)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFE2AB),
                                    Color(0xFFFFC758)
                                ),
                                start = Offset.Zero,
                                end = Offset(500f, 300f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { navController.navigate("socialHolderScreen") }
                )
                FeatureCard(
                    achievementIcon,
                    "Achievement",
                    modifier = cardModifier
                        .align(Alignment.BottomStart)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF95DDE0),
                                    Color(0xFF239CA1)
                                ),
                                start = Offset.Zero,
                                end = Offset(500f, 300f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { navController.navigate("achievementListScreen") }
                )
                FeatureCard(
                    analyticalIcon,
                    "Analytical",
                    modifier = cardModifier
                        .align(Alignment.BottomEnd)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFE9AFF8),
                                    Color(0xFFD561F2)
                                ),
                                start = Offset.Zero,
                                end = Offset(500f, 300f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { navController.navigate("analyticalScreen") }
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Logout Button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        authHandler.signOut()
                        navController.navigate("startScreen")
                    },
                    colors = ButtonDefaults.buttonColors(Color.Red, Color.White)
                ) {
                    Text(text = "Logout", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ManageStrollerCard(context: Context, modifier: Modifier = Modifier){
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF95DDE0),
                        Color(0xFF40B5BA)
                    ),
                    start = Offset.Zero,
                    end = Offset(1000f, -300f)
                )
            )
    ){
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            val strollerIconBitmap = ImageManipulator.vectorToBitmap(context, R.drawable.icon_stroller_2)
            Image(
                modifier = Modifier
                    .size(40.dp),
                painter = BitmapPainter(strollerIconBitmap.asImageBitmap()),
                contentDescription = "Stroller Icon"
            )
            Text(text = "Manage Stroller", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FeatureCard(iconBitmap: Bitmap, text: String, modifier: Modifier = Modifier){
    Box(
        modifier = modifier
            .aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterStart),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Image(
                modifier = Modifier
                    .size(60.dp)
                    .padding(8.dp),
                painter = BitmapPainter(iconBitmap.asImageBitmap()),
                contentDescription = "Stroller Icon"
            )
            Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}