package com.example.autismstroller.functional

import android.bluetooth.BluetoothGattCharacteristic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class BLEListenerHandlerTest {

    // 1. Mocks
    // We don't need a real GATT connection, just a fake provider that returns null
    // (since we are only testing the parsing logic here)
    private val mockGattProvider = { null }
    private val mockCharacteristic: BluetoothGattCharacteristic = mock()

    private val serviceUUID = UUID.randomUUID()
    private val charUUID = UUID.randomUUID()

    private lateinit var handler: BLEListenerHandler
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        handler = BLEListenerHandler(mockGattProvider, charUUID, serviceUUID)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onCharacteristicChanged parses IP address correctly`() = runTest {
        // GIVEN: A characteristic that contains an IP string
        val ipString = "IP:192.168.0.105"
        whenever(mockCharacteristic.uuid).doReturn(charUUID)
        whenever(mockCharacteristic.value).doReturn(ipString.toByteArray(Charsets.UTF_8))

        // WHEN: The characteristic changes
        handler.onCharacteristicChanged(mockCharacteristic)

        // THEN: The StateFlow should update with the IP
        assertEquals("192.168.0.105", handler.cameraIP.value)
    }

    @Test
    fun `onCharacteristicChanged parses Sensor Data correctly`() = runTest {
        // GIVEN: A sensor data string "T:25.5,G:120,D:1.50"
        val sensorString = "T:25.5,G:120,D:1.50"
        whenever(mockCharacteristic.uuid).doReturn(charUUID)
        whenever(mockCharacteristic.value).doReturn(sensorString.toByteArray(Charsets.UTF_8))

        // WHEN
        handler.onCharacteristicChanged(mockCharacteristic)

        // THEN
        val data = handler.sensorData.value
        assertEquals("25.5", data.temp)
        assertEquals("120", data.gas)
        assertEquals("1.50", data.dist)
    }

    @Test
    fun `onCharacteristicChanged ignores wrong UUIDs`() = runTest {
        // GIVEN: A characteristic with a DIFFERENT UUID
        val wrongUUID = UUID.randomUUID()
        whenever(mockCharacteristic.uuid).doReturn(wrongUUID)
        whenever(mockCharacteristic.value).doReturn("IP:1.1.1.1".toByteArray())

        // WHEN
        handler.onCharacteristicChanged(mockCharacteristic)

        // THEN: IP should remain null
        assertNull(handler.cameraIP.value)
    }
}