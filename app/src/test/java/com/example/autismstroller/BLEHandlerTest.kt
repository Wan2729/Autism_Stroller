package com.example.autismstroller.functional

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BLEHandlerTest {

    private val mockContext: Context = mock()
    private val mockScanner: BluetoothLeScanner = mock()
    private val mockDevice: BluetoothDevice = mock()
    private val mockGatt: BluetoothGatt = mock()

    // Initialize with our fake scanner provider
    private val handler = BLEHandler(mockContext) { mockScanner }

    @Test
    fun `connectToDeviceName starts scanning`() {
        // WHEN
        handler.connectToDeviceName("Stroller_Control")

        // THEN
        verify(mockScanner).startScan(any())
    }

    @Test
    fun `scanning stops and connects when correct device found`() {
        // GIVEN: We need to capture the callback that the handler creates
        // We trigger startScan so the callback is created
        handler.connectToDeviceName("Stroller_Control")

        // Capture the callback
        val callbackCaptor = org.mockito.ArgumentCaptor.forClass(ScanCallback::class.java)
        verify(mockScanner).startScan(callbackCaptor.capture())
        val scanCallback = callbackCaptor.value

        // PREPARE: A scan result with the correct name
        val mockResult: ScanResult = mock()
        whenever(mockResult.device).thenReturn(mockDevice)
        whenever(mockDevice.name).thenReturn("Stroller_Control")

        // Mock the connection call
        // Note: device.connectGatt is final in some android versions, requires 'mockito-inline' (which you added!)
        whenever(mockDevice.connectGatt(any(), eq(false), any())).thenReturn(mockGatt)

        // WHEN: We simulate finding the device
        scanCallback.onScanResult(1, mockResult)

        // THEN
        // 1. Should stop scanning (to save battery)
        verify(mockScanner).stopScan(scanCallback)
        // 2. Should connect to the device
        verify(mockDevice).connectGatt(any(), eq(false), any())
    }
}