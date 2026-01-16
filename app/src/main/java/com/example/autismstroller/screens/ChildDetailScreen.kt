package com.example.autismstroller.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.autismstroller.R
import com.example.autismstroller.functional.ChildHandler
import com.example.autismstroller.models.Child
import com.example.autismstroller.utilities.ImageManipulator
import com.example.autismstroller.utilities.ImageManipulator.encodeImageToBase64
import com.example.autismstroller.utilities.ImageManipulator.resizeBitmap
import com.example.autismstroller.utilities.ImageManipulator.uriToBitmap
import java.io.ByteArrayOutputStream

// ---------------------------------------------------------
// SCREEN 2: CHILD DETAIL / EDIT SCREEN (Updated)
// ---------------------------------------------------------
@Composable
fun ChildDetailScreen(navController: NavController, childId: String?) {
    val context = LocalContext.current
    val childHandler: ChildHandler = viewModel()

    // ── Default Random Icon Logic ──
    val profileIcons = listOf(
        R.drawable.icon_defaultprofile1,
        R.drawable.icon_defaultprofile2,
        R.drawable.icon_defaultprofile3,
        R.drawable.icon_defaultprofile4
    )
    val randomBitmap = remember { profileIcons.random() }
    val defaultBitmap = ImageManipulator.vectorToBitmap(context, randomBitmap)
    val defaultImageString = ImageManipulator.encodeImageToBase64(defaultBitmap)

    // ── State Variables ──
    val isNewChild = childId == "new" || childId == null
    var isEditing by remember { mutableStateOf(isNewChild) }

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var notes by remember { mutableStateOf("") }

    // Holds the Base64 string for saving
    var currentProfilePictureString by remember { mutableStateOf(defaultImageString) }
    // Holds the actual Bitmap for display
    var currentDisplayBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var genderExpanded by remember { mutableStateOf(false) }

    // ── 1. Image Picker & Processing ──
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Convert URI to Bitmap -> Resize -> Base64
            val originalBitmap = uriToBitmap(context, it)
            if (originalBitmap != null) {
                // Resize to ~300x300 to keep Base64 string small (<500KB)
                val resizedBitmap = resizeBitmap(originalBitmap, 300, 300)

                // Update State
                currentDisplayBitmap = resizedBitmap
                currentProfilePictureString = encodeImageToBase64(resizedBitmap)
            }
        }
    }

    // ── 2. Permission Launcher ──
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            galleryLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Permission denied. Cannot pick image.", Toast.LENGTH_SHORT).show()
        }
    }

    // ── 3. Data Loading ──
    LaunchedEffect(childId) {
        if (!isNewChild && childId != null) {
            childHandler.getChildById(childId) { child ->
                if (child != null) {
                    name = child.name
                    age = child.age.toString()
                    gender = child.gender
                    notes = child.notes

                    if (child.profilePicture.isNotEmpty()) {
                        currentProfilePictureString = child.profilePicture
                        // Decode existing Base64 to Bitmap for display
                        currentDisplayBitmap = ImageManipulator.decodeBase64ToBitmap(child.profilePicture)
                    }
                } else {
                    Toast.makeText(context, "Error loading child", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // If new child, set the random default immediately
            currentDisplayBitmap = defaultBitmap
            currentProfilePictureString = defaultImageString
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── Header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier
                    .size(28.dp)
                    .clickable { navController.popBackStack() }
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (isNewChild) "Add Child" else if (isEditing) "Edit Child" else "Child Details",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Big Avatar Profile (Clickable) ──
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFBDCE9), Color(0xFFD94583))
                        )
                    )
                    .clickable {
                        // Only allow changing photo in Edit Mode
                        if (isEditing) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                            } else {
                                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (currentDisplayBitmap != null) {
                    Image(
                        bitmap = currentDisplayBitmap!!.asImageBitmap(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = Color.White
                    )
                }

                // Show a small camera icon overlay if editing
                if (isEditing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Edit",
                            tint = Color.White,
                            modifier = Modifier.padding(12.dp).size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        // ── Form Fields ──
        Text("Personal Information", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            readOnly = !isEditing
        )

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = age,
                onValueChange = { if (it.all { char -> char.isDigit() }) age = it },
                label = { Text("Age") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                readOnly = !isEditing
            )

            Spacer(Modifier.width(16.dp))

            // Gender Dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = {},
                    label = { Text("Gender") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = {
                        if (isEditing) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select")
                        }
                    }
                )

                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { genderExpanded = true }
                    )
                }

                DropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false }
                ) {
                    DropdownMenuItem(text = { Text("Male") }, onClick = { gender = "Male"; genderExpanded = false })
                    DropdownMenuItem(text = { Text("Female") }, onClick = { gender = "Female"; genderExpanded = false })
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Diagnosis / Notes") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            readOnly = !isEditing
        )

        Spacer(Modifier.height(40.dp))

        // ── Save Button ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF95DDE0), Color(0xFF239CA1))
                    )
                )
                .clickable {
                    if (isEditing) {
                        val childToSave = Child(
                            id = childId ?: "",
                            parentId = "",
                            name = name,
                            age = age.toIntOrNull() ?: 0,
                            gender = gender,
                            notes = notes,
                            profilePicture = currentProfilePictureString // Saves the updated Base64
                        )

                        childHandler.saveChild(
                            child = childToSave,
                            onSuccess = {
                                Toast.makeText(context, "Child Saved Successfully", Toast.LENGTH_SHORT).show()
                                if (isNewChild) navController.popBackStack() else isEditing = false
                            },
                            onError = { e -> Toast.makeText(context, "Error: $e", Toast.LENGTH_LONG).show() }
                        )
                    } else {
                        isEditing = true
                    }
                }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isEditing) "Save Profile" else "Edit Profile",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // ── Delete Button ──
        if(!isNewChild) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        childHandler.deleteChild(childId ?: "", {
                            Toast.makeText(context, "Profile Deleted", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }, { })
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Delete Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red)
            }
        }
    }
}