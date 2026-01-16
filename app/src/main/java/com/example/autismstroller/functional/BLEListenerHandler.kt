package com.example.autismstroller.functional

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.autismstroller.models.StrollerSensors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BLEListenerHandler(
    private val gattProvider: () -> BluetoothGatt?,
    private val notifyCharacteristicUUID: UUID,
    private val serviceUUID: UUID
){

    private val _sensorData = MutableStateFlow(StrollerSensors())
    val sensorData: StateFlow<StrollerSensors> = _sensorData.asStateFlow()

    // 1. Add StateFlow for Camera IP
    private val _cameraIP = MutableStateFlow<String?>(null)
    val cameraIP: StateFlow<String?> = _cameraIP.asStateFlow()

    private val _gpsLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val gpsLocation: StateFlow<Pair<Double, Double>?> = _gpsLocation.asStateFlow()

    // Call this immediately after connection!
    @SuppressLint("MissingPermission")
    fun enableNotifications() {
        val gatt = gattProvider()
        if (gatt == null) {
            Log.e("BLEListener", "Gatt is null, cannot enable notifications")
            return
        }

        val service = gatt.getService(serviceUUID)
        val characteristic = service?.getCharacteristic(notifyCharacteristicUUID)

        if (characteristic != null) {
            // 1. Enable locally
            gatt.setCharacteristicNotification(characteristic, true)

            // 2. Write to Descriptor (Required for Android to receive updates)
            val uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            val descriptor = characteristic.getDescriptor(uuid)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
            Log.d("BLEListener", "Notifications enabled for $notifyCharacteristicUUID")
        } else {
            Log.e("BLEListener", "Characteristic not found!")
        }
    }

    fun onCharacteristicChanged(characteristic: BluetoothGattCharacteristic) {
        if (characteristic.uuid == notifyCharacteristicUUID) {
            val rawData = characteristic.value.toString(Charsets.UTF_8)
            // rawData might be "T:24.5,G:120,D:1.50" OR "IP:192.168.1.50"
            parseData(rawData)
        }
    }

    private fun parseData(data: String) {
        try {
            // 2. Check for IP Address packet first
            if (data.startsWith("IP:")) {
                val ip = data.substring(3).trim() // Remove "IP:"
                _cameraIP.value = ip
                Log.d("BLEListener", "Camera IP Detected: $ip")
                return // Stop parsing, this isn't sensor data
            }

            if (data.startsWith("L:")) {
                val parts = data.substring(2).split(",")
                if (parts.size == 2) {
                    val lat = parts[0].toDoubleOrNull()
                    val lng = parts[1].toDoubleOrNull()

                    // Filter out 0.0/0.0 which usually means "No GPS Fix" on hardware
                    if (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) {
                        _gpsLocation.value = Pair(lat, lng)
                        Log.d("BLEListener", "GPS Updated: $lat, $lng")
                    }
                }
                return
            }

            // 3. Existing Sensor Parsing Logic
            val parts = data.split(",")
            var t = _sensorData.value.temp // Keep old value as default
            var g = _sensorData.value.gas
            var d = _sensorData.value.dist

            for (part in parts) {
                when {
                    part.startsWith("T:") -> t = part.substring(2)
                    part.startsWith("G:") -> g = part.substring(2)
                    part.startsWith("D:") -> d = part.substring(2)
                }
            }
            _sensorData.value = StrollerSensors(temp = t, gas = g, dist = d)
        } catch (e: Exception) {
            Log.e("BLEListener", "Parse error for data '$data': $e")
        }
    }

    fun clearIP() {
        _cameraIP.value = null
        Log.d("BLEListener", "Camera IP cleared")
    }
}