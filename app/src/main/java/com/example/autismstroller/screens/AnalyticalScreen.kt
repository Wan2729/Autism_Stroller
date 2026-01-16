package com.example.autismstroller.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.autismstroller.functional.AnalyticalHandler
import com.example.autismstroller.functional.ChildHandler
import com.example.autismstroller.functional.GraphPoint
import com.example.autismstroller.functional.SongHandler
import com.example.autismstroller.functional.supabase
import com.example.autismstroller.models.Child
import com.example.autismstroller.reusables.InfoBox
import com.example.autismstroller.utilities.AppColors
import com.example.autismstroller.utilities.ImageManipulator
import com.example.autismstroller.utilities.PdfUtility

@Composable
fun AnalyticalScreen(navController: NavController) {
    val context = LocalContext.current

    // Handlers
    val childHandler: ChildHandler = viewModel()
    val analyticalHandler: AnalyticalHandler = viewModel()
    val songHandler: SongHandler = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // REPLACE 'YourSupabaseObject.client' with your actual client instance
                return SongHandler(supabase) as T
            }
        }
    )

    // State
    val childrenList by childHandler.childrenList.collectAsState()
    val graphData by analyticalHandler.graphData.collectAsState()
    val summaryText by analyticalHandler.summaryText.collectAsState()

    var selectedChild by remember { mutableStateOf<Child?>(null) }
    var selectedCategory by remember { mutableStateOf("Music") }
    val songsList by songHandler.songs.collectAsState()
    var showPdfDialog by remember { mutableStateOf(false) }

    // Fetch children on load
    LaunchedEffect(Unit) {
        childHandler.fetchChildren()
    }

    // Auto-select first child if none selected
    LaunchedEffect(childrenList) {
        if (selectedChild == null && childrenList.isNotEmpty()) {
            selectedChild = childrenList.first()
        }
    }

    // Process stats whenever selection changes
    LaunchedEffect(selectedChild, selectedCategory) {
        selectedChild?.let {
            analyticalHandler.processStat(it, selectedCategory, songsList)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
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
            Text(text = "Analytics", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))

        // ── 1. Child Selector (Horizontal Row) ──
        Text("Select Child", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(childrenList) { child ->
                ChildAvatarItem(
                    child = child,
                    isSelected = child.id == selectedChild?.id,
                    onClick = { selectedChild = child }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        if (showPdfDialog && selectedChild != null) {
            AlertDialog(
                onDismissRequest = { showPdfDialog = false },
                title = { Text("Generate Report") },
                text = {
                    Text("Do you want to generate a PDF report for ${selectedChild!!.name}? This will be saved to your Downloads folder.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // 1. Generate PDF
                            PdfUtility.generateAndSavePdf(context, selectedChild!!, songsList)
                            // 2. Close Dialog
                            showPdfDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.SoftBlue)
                    ) {
                        Text("Download PDF")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPdfDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        if (selectedChild != null) {
            // ── 2. Stat Category Selector ──
            Text("Statistic", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(analyticalHandler.statCategories) { category ->
                    CategoryPill(
                        text = category,
                        isSelected = category == selectedCategory,
                        onClick = { selectedCategory = category }
                    )
                }
            }

            Spacer(Modifier.height(32.dp)) // Extra space to separate from charts

            Button(
                onClick = { showPdfDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.SoftBlue),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Download Report as PDF",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── 3. Main Chart Area ──
            if (selectedCategory == "Overview") {
                // Render Simple Cards for Overview
                OverviewSection(selectedChild!!)
            } else {
                // Render Graph for Categories
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF5F5F5)) // Light Gray BG
                        .padding(16.dp)
                ) {
                    if (graphData.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No data available yet.", color = Color.Gray)
                        }
                    } else {
                        SimpleBarChart(data = graphData)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Summary Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.SoftBlue.copy(alpha = 0.2f))
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Insight", fontWeight = FontWeight.Bold, color = AppColors.SoftBlue)
                        Text(summaryText, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ── SUB-COMPONENTS ──

@Composable
fun ChildAvatarItem(child: Child, isSelected: Boolean, onClick: () -> Unit) {
    val avatarBitmap = remember(child.profilePicture) {
        ImageManipulator.decodeBase64ToBitmap(child.profilePicture)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 64.dp else 56.dp) // Pop effect
                .clip(CircleShape)
                .background(if (isSelected) AppColors.SoftBlue else AppColors.SoftPink)
                .padding(if (isSelected) 4.dp else 0.dp) // Border effect
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (avatarBitmap != null) {
                Image(
                    bitmap = avatarBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Face, contentDescription = null, tint = Color.White)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = child.name.take(8) + if(child.name.length>8)".." else "",
            fontSize = 12.sp,
            fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun CategoryPill(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (isSelected)
                    Brush.linearGradient(listOf(AppColors.SoftBlue, Color(0xFF239CA1)))
                else
                    Brush.linearGradient(listOf(Color.White, Color.White))
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Black,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun OverviewSection(child: Child) {
    val stats = child.childStats
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoBox(
                title = "Distance",
                value = "${"%.1f".format(stats.totalDistanceTravelled)}m",
                isEditing = false,
                modifier = Modifier.weight(1f)
            )
            InfoBox(
                title = "Total Time",
                value = "${stats.totalTimeInStrollerMinutes} mins",
                isEditing = false,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoBox(
                title = "Avg Temp",
                value = if(stats.sensorReadingCount > 0)
                    "${"%.1f".format(stats.tempExposureSum / stats.sensorReadingCount)}°C"
                else "N/A",
                isEditing = false,
                modifier = Modifier.weight(1f)
            )
            InfoBox(
                title = "Avg CO",
                value = if(stats.sensorReadingCount > 0)
                    "${"%.1f".format(stats.coExposureSum / stats.sensorReadingCount)} ppm"
                else "N/A",
                isEditing = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── CUSTOM GRAPH COMPONENT (No libraries required) ──
@Composable
fun SimpleBarChart(data: List<GraphPoint>) {
    // 1. Calculate Max safely (avoid divide by zero if all data is empty/zero)
    val maxVal = data.maxOfOrNull { it.value } ?: 1f
    val safeMax = if (maxVal == 0f) 1f else maxVal

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { point ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Value Label
                Text(
                    text = point.value.toInt().toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )

                Spacer(Modifier.height(4.dp))

                // The Graph Bar Area (Takes all available vertical space)
                Box(
                    modifier = Modifier
                        .weight(1f) // This fills the gap between the text labels
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter // Align bar to the bottom
                ) {
                    // Calculate height fraction (0.0 to 1.0)
                    val barHeightFraction = point.value / safeMax

                    if (barHeightFraction > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .fillMaxHeight(barHeightFraction) // Correct way to size bars
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(Color(point.colorHex))
                        )
                    } else {
                        // Optional: Show a tiny gray line for '0' so it's not empty
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(2.dp)
                                .background(Color.LightGray.copy(alpha = 0.5f))
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Axis Label
                Text(
                    text = point.label.take(10),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp
                )
            }
        }
    }
}