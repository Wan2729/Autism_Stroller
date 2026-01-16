package com.example.autismstroller.functional

import android.annotation.SuppressLint
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class BLEHandler(
    private val context: Context,
    private val scannerProvider: (() -> BluetoothLeScanner)? = null
) {

    private val _connectionState = MutableStateFlow(BleConnectionState.IDLE)
    val connectionState: StateFlow<BleConnectionState> = _connectionState

    private var bluetoothGatt: BluetoothGatt? = null
    fun getGatt(): BluetoothGatt? = bluetoothGatt

    private val scanner: BluetoothLeScanner by lazy {
        scannerProvider?.invoke() ?: (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.bluetoothLeScanner
    }
    private val SERVICE_UUID = UUID.fromString(com.example.autismstroller.functional.SERVICE_UUID)

    private var bleListenerHandler: BLEListenerHandler? = null

    private val _isReady = MutableStateFlow(false)
    val espConnected: StateFlow<Boolean> = _isReady
//    val espConnected: StateFlow<Boolean> =
//        connectionState.map { it == BleConnectionState.READY }
//            .stateIn(
//                scope = CoroutineScope(Dispatchers.Default),
//                started = SharingStarted.Eagerly,
//                initialValue = false
//            )

    @SuppressLint("MissingPermission")
    fun connectToDeviceName(name: String) {
        scanner.startScan(object : ScanCallback() {
            override fun onScanResult(type: Int, result: ScanResult) {
                val device = result.device
                if (device.name == name) {
                    scanner.stopScan(this)
                    bluetoothGatt = device.connectGatt(context, false, gattCallback)
                }
            }
        })
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("BLEHandler", "Gatt error: $status")
                _connectionState.value = BleConnectionState.ERROR
                cleanupGatt()
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLEHandler", "Connected to GATT server.")
                _connectionState.value = BleConnectionState.CONNECTED

                // FIX 1: Request larger MTU size (512 bytes) to prevent data truncation
                gatt.requestMtu(512)

                // Note: We normally wait for onMtuChanged before discovering services,
                // but for simple ESP32 projects, calling discover immediately often works.
                // If unstable, move discoverServices() to onMtuChanged callback.
                gatt.discoverServices()

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BLEHandler", "Disconnected from GATT server.")
                _connectionState.value = BleConnectionState.DISCONNECTED
                cleanupGatt()
            }
        }

        // Callback for MTU change (Optional but good for debugging)
        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            Log.d("BLEHandler", "MTU changed to: $mtu")
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.ERROR
                return
            }

            val service = gatt.getService(SERVICE_UUID)
            if (service == null) {
                _connectionState.value = BleConnectionState.ERROR
                return
            }

            // Optional: validate characteristic
            val characteristic = service.characteristics.firstOrNull()
            if (characteristic == null) {
                _connectionState.value = BleConnectionState.ERROR
                return
            }

            _connectionState.value = BleConnectionState.READY
            _isReady.value = true
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            bleListenerHandler?.onCharacteristicChanged(characteristic)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            // Forward to the same listener logic
            bleListenerHandler?.onCharacteristicChanged(characteristic)
        }

    }

    fun setListener(listener: BLEListenerHandler?) {
        bleListenerHandler = listener
    }

    @SuppressLint("MissingPermission")
    private fun cleanupGatt() {
        _isReady.value = false
        bluetoothGatt?.close()
        bluetoothGatt = null
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
