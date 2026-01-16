package com.example.autismstroller.functional

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class A2DPReciever(
    private val bleHandler: BLEHandler
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action ==
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) {

            val state =
                intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
            val device =
                intent.getParcelableExtra<BluetoothDevice>(
                    BluetoothDevice.EXTRA_DEVICE
                )

            if (state == BluetoothProfile.STATE_CONNECTED &&
                device?.name == "Stroller_Control") {

                // 🔥 AUTO CONNECT BLE
                bleHandler.connectToDeviceName(device.name)
            }
        }
    }
}

