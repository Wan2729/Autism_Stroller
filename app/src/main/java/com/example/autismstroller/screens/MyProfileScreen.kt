package com.example.autismstroller.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.autismstroller.R
import com.example.autismstroller.functional.UserHandler
import com.example.autismstroller.functional.UserState
import com.example.autismstroller.reusables.FriendsBox
import com.example.autismstroller.reusables.GenderBox
import com.example.autismstroller.reusables.InfoBox
import com.example.autismstroller.utilities.AppColors
import com.example.autismstroller.utilities.ImageManipulator

@Composable
fun MyProfileScreen(navController: NavController, userId: String) {
    val context = LocalContext.current
    val userHandler: UserHandler = viewModel()
    val userState by userHandler.userstate.collectAsState()

    // ── State for Edit Mode ──
    var isEditing by remember { mutableStateOf(false) }

    // ── NEW: Handle Back Gesture ──
    // If editing, back button cancels edit. If not editing, it behaves normally.
    BackHandler(enabled = isEditing) {
        isEditing = false
    }

    // ── Local Form State ──
    var name by remember { mutableStateOf("") }
    var ageString by remember { mutableStateOf("") }
    var genderString by remember { mutableStateOf("Male") }
    var description by remember { mutableStateOf("") }
    var currentProfilePicBase64 by remember { mutableStateOf("") }
    var currentDisplayBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // ── Load Data ──
    LaunchedEffect(userId) {
        userHandler.getUserDetails(userId)
    }

    // ── Sync State when UserDetails Loads ──
    val userDetails = (userState as? UserState.UserDetails)?.userDetail
    LaunchedEffect(userDetails) {
        if (userDetails != null) {
            name = userDetails.name
            ageString = userDetails.age.toString()
            genderString = if (userDetails.genderMale) "Male" else "Female"
            description = userDetails.description
            currentProfilePicBase64 = userDetails.profilePicture
            currentDisplayBitmap = ImageManipulator.decodeBase64ToBitmap(userDetails.profilePicture)
        }
    }

    // ── Image Picker Logic ──
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val original = ImageManipulator.uriToBitmap(context, it)
            if (original != null) {
                val resized = ImageManipulator.resizeBitmap(original, 300, 300)
                currentDisplayBitmap = resized
                currentProfilePicBase64 = ImageManipulator.encodeImageToBase64(resized)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) galleryLauncher.launch("image/*")
        else Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    if (userDetails == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // ADDED: Light gray background to make white shadows pop
                .background(Color(0xFFF5F6F8))
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header Row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "My Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold)

                IconButton(onClick = {
                    if (isEditing) {
                        // SAVE ACTION
                        val finalAge = ageString.toIntOrNull() ?: 0
                        val isMale = genderString == "Male"

                        userHandler.updateUserProfile(
                            userId = userId,
                            name = name,
                            age = finalAge,
                            genderMale = isMale,
                            description = description,
                            profilePictureBase64 = currentProfilePicBase64,
                            onSuccess = {
                                isEditing = false
                                Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                            },
                            onError = { e ->
                                Toast.makeText(context, "Error: $e", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        isEditing = true
                    }
                }) {
                    Icon(
                        imageVector = if(isEditing) Icons.Default.Send else Icons.Default.Edit,
                        contentDescription = if(isEditing) "Save" else "Edit",
                        tint = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Profile Card (KEPT FLAT/TRANSPARENT as requested) ──
            ProfileCard(
                context = context,
                isEditing = isEditing,
                name = name,
                onNameChange = { name = it },
                level = userDetails.level,
                displayBitmap = currentDisplayBitmap,
                onAvatarClick = {
                    if (isEditing) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Description (ADDED SHADOW) ──
            InfoBox(
                title = "Description",
                value = description,
                isEditing = isEditing,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp)) // Increased spacing slightly for shadow room

            // ── Stats Row (ADDED SHADOW) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp) // Use spacedBy for cleaner layout
            ) {
                // Age
                InfoBox(
                    title = "Age",
                    value = ageString,
                    isEditing = isEditing,
                    onValueChange = { if(it.all { char -> char.isDigit() }) ageString = it },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )

                // Gender
                GenderBox(
                    gender = genderString,
                    isEditing = isEditing,
                    onGenderChange = { genderString = it },
                    modifier = Modifier.weight(1f)
                )

                // Friends
                FriendsBox(
                    count = userDetails.friends.size,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { navController.navigate("friendListScreen/${userId}") }
                )
            }
        }
    }
}

// ── SUB-COMPONENTS ──

@Composable
fun ProfileCard(
    context: Context,
    isEditing: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    level: Int,
    displayBitmap: Bitmap?,
    onAvatarClick: () -> Unit
) {
    // Left flat as requested
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            if (displayBitmap != null) {
                Image(
                    painter = androidx.compose.ui.graphics.painter.BitmapPainter(displayBitmap.asImageBitmap()),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.icon_loading_pfp),
                    contentDescription = "Default",
                    modifier = Modifier.size(40.dp)
                )
            }

            if (isEditing) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Edit", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            if (isEditing) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(text = name.ifEmpty { "No Name" }, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(text = "Level $level", fontSize = 14.sp, color = Color.Gray)
        }
    }
}