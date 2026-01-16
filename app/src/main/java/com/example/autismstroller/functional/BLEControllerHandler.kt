package com.example.autismstroller.functional

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autismstroller.models.LED
import com.example.autismstroller.utilities.LEDColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BLEControllerHandler(
    private val gattProvider: () -> BluetoothGatt?,
    private val characteristicUUID: UUID,
    private val serviceUUID: UUID
) : ViewModel() {

    private val _lightOn = MutableStateFlow(false)
    val lightOn: StateFlow<Boolean> = _lightOn
    private val _lightColor = MutableStateFlow(LEDColors.Black)
    val lightColor: StateFlow<Int> = _lightColor
    private val _lightAnimation = MutableStateFlow(1)
    val lightAnimation: StateFlow<Int> = _lightAnimation
    private val _led = MutableStateFlow(LED(0,0,0))
    val led: StateFlow<LED> = _led

    @SuppressLint("MissingPermission")
    fun toggleLED(hue: Int, saturation: Int, value: Int, animation: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val gatt = gattProvider() ?: return@launch
            val service = gatt.getService(serviceUUID) ?: return@launch
            val characteristic = service.getCharacteristic(characteristicUUID) ?: return@launch
            val commandStr: String
            if(!_lightOn.value){
                commandStr = "L:0_0_0_0"
            }else{
                commandStr = "L:"+hue+"_"+saturation+"_"+value+"_"+animation
            }
                val commandByte = commandStr.toByteArray(Charsets.UTF_8)
                characteristic.value = commandByte
            val success = gatt.writeCharacteristic(characteristic)
            Log.d("BLE Write", "Write success: $success")

            _lightOn.value = !_lightOn.value
        }
    }

    @SuppressLint("MissingPermission")
    fun toggleLED(hsv: String, lightOn: Boolean, animation: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val gatt = gattProvider() ?: return@launch
            val service = gatt.getService(serviceUUID) ?: return@launch
            val characteristic = service.getCharacteristic(characteristicUUID) ?: return@launch
            val commandStr: String
            commandStr = if(!lightOn){
                "L:0_0_0_0"
            }else{
                "L:"+hsv+"_"+animation
            }
            val commandByte = commandStr.toByteArray(Charsets.UTF_8)
            characteristic.value = commandByte
            val success = gatt.writeCharacteristic(characteristic)
            Log.d("BLE Write", "Write success: $success")

            _lightOn.value = !_lightOn.value
        }
    }

    @SuppressLint("MissingPermission")
    fun playSpeaker() {
        viewModelScope.launch(Dispatchers.IO) {
            val gatt = gattProvider() ?: return@launch
            val service = gatt.getService(serviceUUID) ?: return@launch
            val characteristic = service.getCharacteristic(characteristicUUID) ?: return@launch

            val command = byteArrayOf(0x33) // '3'
            characteristic.value = command
            gatt.writeCharacteristic(characteristic)

            _lightOn.value = !_lightOn.value
        }
    }

    @SuppressLint("MissingPermission")
    fun sendWiFiCredentials(ssid: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val gatt = gattProvider() ?: return@launch
            val service = gatt.getService(serviceUUID) ?: return@launch
            val characteristic = service.getCharacteristic(characteristicUUID) ?: return@launch

            // Command Format: W:SSID,PASSWORD
            val commandStr = "W:$ssid,$password"

            val commandByte = commandStr.toByteArray(Charsets.UTF_8)
            characteristic.value = commandByte
            val success = gatt.writeCharacteristic(characteristic)
            Log.d("BLE Write", "WiFi Credentials sent: $success")
        }
    }

    @SuppressLint("MissingPermission")
    fun setDisplayMode(mode: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val gatt = gattProvider() ?: return@launch
            val service = gatt.getService(serviceUUID) ?: return@launch
            val characteristic = service.getCharacteristic(characteristicUUID) ?: return@launch

            // Command Format: V:mode (e.g., V:1, V:2, or V:-1)
            val commandStr = "V:$mode"

            val commandByte = commandStr.toByteArray(Charsets.UTF_8)
            characteristic.value = commandByte
            val success = gatt.writeCharacteristic(characteristic)
            Log.d("BLE Write", "Display command sent ($commandStr): $success")
        }
    }
}
