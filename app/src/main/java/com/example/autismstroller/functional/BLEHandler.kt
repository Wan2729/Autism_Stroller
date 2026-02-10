package com.example.autismstroller.functional

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class BLEHandler(
    private val context: Context
) {

    private val _connectionState = MutableStateFlow(BleConnectionState.IDLE)
    val connectionState: StateFlow<BleConnectionState> = _connectionState

    private var bluetoothGatt: BluetoothGatt? = null
    fun getGatt(): BluetoothGatt? = bluetoothGatt

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter = bluetoothManager.adapter

    // Use the scanner from the adapter
    private val scanner: BluetoothLeScanner?
        get() = adapter.bluetoothLeScanner

    private val SERVICE_UUID_VAL = UUID.fromString(com.example.autismstroller.functional.SERVICE_UUID)
    private var bleListenerHandler: BLEListenerHandler? = null

    private val _isReady = MutableStateFlow(false)
    val espConnected: StateFlow<Boolean> = _isReady

    @SuppressLint("MissingPermission")
    fun connectToDeviceName(name: String) {
        // Launch in IO scope to stop freezing the UI
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            if (adapter.isEnabled.not()) {
                Log.e("BLEHandler", "Bluetooth is off")
                return@launch
            }

            // 1. Check Bonded Devices (Preferred)
            val bondedDevice = adapter.bondedDevices.find { it.name == name }
            if (bondedDevice != null) {
                Log.d("BLEHandler", "Device $name found in paired devices. Connecting LE...")
                connectToDevice(bondedDevice) // This now calls the updated version with TRANSPORT_LE
                return@launch
            }

            // 2. Check Connected Devices
            val connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
            val connectedDevice = connectedDevices.find { it.name == name }
            if (connectedDevice != null) {
                Log.d("BLEHandler", "Device $name is already GATT connected. Re-hooking...")
                connectToDevice(connectedDevice)
                return@launch
            }

            // 3. Scan (Fallback)
            // Note: Scanning must still happen on Main thread safely or be carefully managed, 
            // but typically we want to avoid scanning if bonded.
            // If you MUST scan, switch back to Main for the scanner start
            withContext(Dispatchers.Main) {
                startScan(name)
            }
        }
    }

    // Helper for scanning to keep code clean
    @SuppressLint("MissingPermission")
    private fun startScan(name: String) {
        Log.d("BLEHandler", "Starting scan for $name...")
        _connectionState.value = BleConnectionState.SCANNING
        scanner?.startScan(object : ScanCallback() {
            override fun onScanResult(type: Int, result: ScanResult) {
                if (result.device.name == name) {
                    scanner?.stopScan(this)
                    connectToDevice(result.device)
                }
            }
            override fun onScanFailed(errorCode: Int) {
                _connectionState.value = BleConnectionState.ERROR
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        _connectionState.value = BleConnectionState.CONNECTING

        // CHANGE IS HERE: Add 'BluetoothDevice.TRANSPORT_LE'
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("BLEHandler", "Gatt error: $status")
                _connectionState.value = BleConnectionState.ERROR
                gatt.close() // Close properly on error
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLEHandler", "Connected to GATT server.")
                _connectionState.value = BleConnectionState.CONNECTED
                bluetoothGatt = gatt
                gatt.requestMtu(512)
                gatt.discoverServices()

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BLEHandler", "Disconnected from GATT server.")
                _connectionState.value = BleConnectionState.DISCONNECTED
                _isReady.value = false
                bluetoothGatt?.close()
                bluetoothGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Double check service existence
                val service = gatt.getService(SERVICE_UUID_VAL)
                if(service != null) {
                    _connectionState.value = BleConnectionState.READY
                    _isReady.value = true
                } else {
                    Log.e("BLEHandler", "Target Service not found!")
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            bleListenerHandler?.onCharacteristicChanged(characteristic)
        }

        // Handle the new onCharacteristicChanged signature for newer Android versions if needed
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            bleListenerHandler?.onCharacteristicChanged(characteristic)
        }
    }

    fun setListener(listener: BLEListenerHandler?) {
        bleListenerHandler = listener
    }
}

enum class BleConnectionState {
    IDLE,
    SCANNING,
    CONNECTING,
    CONNECTED,          // GATT connected
    READY,              // Services + characteristic available
    DISCONNECTED,
    ERROR
}