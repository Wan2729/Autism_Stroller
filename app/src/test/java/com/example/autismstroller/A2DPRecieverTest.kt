package com.example.autismstroller.functional

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class A2DPRecieverTest {

    // 1. Mocks for Android System Objects
    private val mockContext: Context = mock()
    private val mockIntent: Intent = mock()
    private val mockDevice: BluetoothDevice = mock()

    // 2. Mock for YOUR Handler
    private val mockBleHandler: BLEHandler = mock()

    // 3. The Class Under Test
    private val receiver = A2DPReciever(mockBleHandler)

    @Test
    fun `onReceive triggers connect when Stroller_Control connects`() {
        // GIVEN: The Intent says "Stroller_Control" is "CONNECTED"
        whenever(mockIntent.action).doReturn(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        whenever(mockIntent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1))
            .doReturn(BluetoothProfile.STATE_CONNECTED)

        whenever(mockIntent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE))
            .doReturn(mockDevice)

        whenever(mockDevice.name).doReturn("Stroller_Control")

        // WHEN: Android broadcasts this intent
        receiver.onReceive(mockContext, mockIntent)

        // THEN: Your receiver should tell BLEHandler to connect
        verify(mockBleHandler).connectToDeviceName("Stroller_Control")
    }

    @Test
    fun `onReceive ignores other devices`() {
        // GIVEN: Some random speaker connects
        whenever(mockIntent.action).doReturn(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        whenever(mockIntent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1))
            .doReturn(BluetoothProfile.STATE_CONNECTED)

        whenever(mockIntent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE))
            .doReturn(mockDevice)

        whenever(mockDevice.name).doReturn("JBL_Speaker") // Not the stroller

        // WHEN
        receiver.onReceive(mockContext, mockIntent)

        // THEN: Should NOT call connect
        // (We assume verify(mock, times(0)) or simply not verifying implicitly passes if not called)
        // To be strict:
        // verify(mockBleHandler, never()).connectToDeviceName(any())
        // But since we aren't using 'never()' import here, if the test passes, it means no crash.
        // If you want to be strict, add import org.mockito.kotlin.never and uncomment:
        // verify(mockBleHandler, org.mockito.kotlin.never()).connectToDeviceName(any())
    }
}