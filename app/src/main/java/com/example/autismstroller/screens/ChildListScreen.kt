package com.example.autismstroller.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.autismstroller.R
import com.example.autismstroller.functional.ChildHandler
import com.example.autismstroller.models.Child
import com.example.autismstroller.utilities.AppColors
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.autismstroller.utilities.ImageManipulator

// ---------------------------------------------------------
// SCREEN 1: CHILD LIST SCREEN
// ---------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildListScreen(navController: NavController) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val childHandler: ChildHandler = viewModel()

    // 2. Collect Data
    val childrenList by childHandler.childrenList.collectAsState()

    // 3. Trigger Fetch on load
    LaunchedEffect(Unit) {
        childHandler.fetchChildren()
    }

    val filteredList = childrenList.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                text = "Manage Children",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Select a child to view details or add a new profile.",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(20.dp))

        // ── Search Bar ──
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
            placeholder = { Text("Search children...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color(0xFFF0F0F0),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(Modifier.height(20.dp))

        // ── List ──
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredList) { child ->
                ChildListItem(
                    child = child,
                    onClick = {
                        // Navigate to Detail Screen, passing ID
                        // Ensure you add arguments to your NavHost definition
                        navController.navigate("childDetailScreen/${child.id}")
                    }
                )
            }

            // Empty State
            if(filteredList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No children found.", color = Color.Gray)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Add Button ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF95DDE0),
                            Color(0xFF239CA1)
                        )
                    )
                )
                .clickable {
                    // Navigate to detail screen with "new" or empty ID
                    navController.navigate("childDetailScreen/new")
                }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Text(
                    text = "Add Children",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ChildListItem(child: Child, onClick: () -> Unit) {
    // 1. Decode the Base64 string to a Bitmap
    val avatarBitmap = remember(child.profilePicture) {
        ImageManipulator.decodeBase64ToBitmap(child.profilePicture)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Avatar Logic ──
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppColors.SoftPink, Color(0xFFD94583)) // Adjusted to match your Pink theme better
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    // Show Actual Profile Picture
                    Image(
                        bitmap = avatarBitmap.asImageBitmap(),
                        contentDescription = "Child Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Show Default Icon
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // ── Name ──
            Text(
                text = child.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}