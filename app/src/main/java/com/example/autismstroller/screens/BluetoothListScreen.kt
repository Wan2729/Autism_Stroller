//package com.example.autismstroller.screens
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.Activity
//import android.content.pm.PackageManager
//import android.os.Build
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.material3.Button
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.Divider
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import androidx.navigation.NavController
//import com.example.autismstroller.functional.BLEHandler
//
//@SuppressLint("MissingPermission")
//@Composable
//fun BluetoothListScreen(navController: NavController, bleHandler: BLEHandler) {
//    val context = LocalContext.current
//    val isScanning by bleHandler.isScanning.collectAsState()
//    val espConnected by bleHandler.espConnected.collectAsState()
//    val deviceList = bleHandler.deviceList
//    val activity = context as? Activity
//    val connectionStatus by bleHandler.connectionStatus.collectAsState()
//
//    // Request permissions
//    LaunchedEffect(Unit) {
//        val permissions = mutableListOf<String>()
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
//                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
//            }
//            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
//            }
//        } else {
//            // For Android 6 to 11
//            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
//            }
//            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
//            }
//        }
//
//        if (permissions.isNotEmpty()) {
//            ActivityCompat.requestPermissions(
//                activity!!,
//                permissions.toTypedArray(),
//                1001
//            )
//        }
//    }
//    LaunchedEffect(Unit) {
//        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
//            bleHandler.initialize(context)
//        }
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        verticalArrangement = Arrangement.spacedBy(16.dp)
//    ) {
//        Text("ESP32 BLE Controller", fontSize = 22.sp, fontWeight = FontWeight.Bold)
//
//        Divider()
//
//        // Connection Status
//        Text(
//            text = if (espConnected) "✅ Connected to ESP32" else "❌ Not Connected",
//            color = if (espConnected) Color(0xFF4CAF50) else Color.Red,
//            fontWeight = FontWeight.Medium
//        )
//
//        Text("Status: $connectionStatus", fontWeight = FontWeight.Bold, color = Color.Blue)
//
//        // Scan Controls
//        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//            Button(onClick = {
//                if (isScanning) bleHandler.stopScan()
//                else bleHandler.startScan()
//            }) {
//                Text(if (isScanning) "Stop Scanning" else "Start Scanning")
//            }
//
//            Button(
//                onClick = { bleHandler.forceReset() },
//                colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
//            ) {
//                Text("Reset BLE")
//            }
//        }
//
//        Divider()
//
//        // Device List
//        Text("Available Devices", fontWeight = FontWeight.SemiBold)
//
//        if (deviceList.isEmpty()) {
//            Text("🔍 No devices found. Try scanning again or move closer.")
//        } else {
//            LazyColumn {
//                itemsIndexed(deviceList) { index, device ->
//                    Column(modifier = Modifier
//                        .fillMaxWidth()
//                        .clickable {
//                            bleHandler.connectToDevice(context, device)
//                        }
//                        .padding(vertical = 8.dp)) {
//                        Text("${device.name ?: "Unnamed"}", fontSize = 18.sp)
//                        Text("Address: ${device.address}", fontSize = 14.sp, color = Color.Gray)
//                    }
//                }
//            }
//        }
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        // LED or Action Button
//        Button(
//            onClick = {
//                // Add your ESP32 command sending logic here
//            },
//            enabled = espConnected,
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text(if (espConnected) "Send LED Toggle Command" else "Connect to a device to control LED")
//        }
//    }
//}
