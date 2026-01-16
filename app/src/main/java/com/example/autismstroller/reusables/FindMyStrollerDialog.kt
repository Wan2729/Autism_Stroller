package com.example.autismstroller.reusables

import android.preference.PreferenceManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.autismstroller.utilities.AppColors
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun FindMyStrollerDialog(
    location: Pair<Double, Double>?, // Null means no GPS data yet
    isSoundPlaying: Boolean,
    onToggleSound: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Initialize OSMDroid Configuration (REQUIRED)
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        // Set user agent to prevent blocking
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(Modifier.fillMaxSize()) {
                // --- Header ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Find My Stroller", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (location != null) {
                            Text(
                                "Last seen: ${location.first}, ${location.second}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // --- Map Section (OSMDROID) ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFEEEEEE))
                ) {
                    if (location != null) {
                        // We use AndroidView to embed the classic View-based MapView
                        AndroidView(
                            factory = { ctx ->
                                MapView(ctx).apply {
                                    setTileSource(TileSourceFactory.MAPNIK)
                                    setMultiTouchControls(true)
                                    controller.setZoom(19.0)
                                }
                            },
                            update = { mapView ->
                                // Update logic when location changes
                                val geoPoint = GeoPoint(location.first, location.second)

                                // Move Camera
                                mapView.controller.animateTo(geoPoint)

                                // Clear old markers and add new one
                                mapView.overlays.clear()
                                val marker = Marker(mapView)
                                marker.position = geoPoint
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                marker.title = "Stroller"
                                marker.snippet = "Lat: ${location.first}, Lng: ${location.second}"
                                // You can set a custom icon here if you want:
                                // marker.icon = ContextCompat.getDrawable(context, R.drawable.icon_stroller_marker)

                                mapView.overlays.add(marker)
                                mapView.invalidate() // Force redraw
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // GRACEFUL HANDLING: No Data
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = AppColors.SoftBlue)
                            Spacer(Modifier.height(16.dp))
                            Text("Waiting for GPS signal...", color = Color.Gray)
                            Text("Ensure stroller is outdoors.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                // --- Footer: Sound Control ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Can't see it? Play a sound.",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = onToggleSound,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSoundPlaying) Color.Red else AppColors.SoftBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = if (isSoundPlaying) Icons.Default.Close else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isSoundPlaying) "Stop Sound" else "Play Alert Sound")
                    }
                }
            }
        }
    }
}