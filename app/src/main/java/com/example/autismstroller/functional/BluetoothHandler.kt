package com.example.autismstroller.functional

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

class BluetoothHandler : ViewModel() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private val _deviceList = mutableStateListOf<BluetoothDevice>()
    val deviceList: List<BluetoothDevice> get() = _deviceList

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _espConnected = MutableStateFlow(false)
    val espConnected: StateFlow<Boolean> = _espConnected

    val connectedSocket: BluetoothSocket? get() = bluetoothSocket

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            device?.let {
                if (!_deviceList.any { it.address == device.address }) {
                    _deviceList.add(device)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(context: Context) {
        _deviceList.clear()
        _isScanning.value = true

        bluetoothAdapter?.bondedDevices?.forEach { device ->
            if (!_deviceList.any { it.address == device.address }) {
                _deviceList.add(device)
            }
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(receiver, filter)
        bluetoothAdapter?.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        bluetoothAdapter?.cancelDiscovery()
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        stopScan()
        val uuid = device.uuids?.firstOrNull()?.uuid ?: UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
        bluetoothAdapter?.cancelDiscovery()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                bluetoothSocket?.connect()
                _espConnected.value = true
            } catch (e: IOException) {
                e.printStackTrace()
                _espConnected.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothSocket?.close()
    }
}