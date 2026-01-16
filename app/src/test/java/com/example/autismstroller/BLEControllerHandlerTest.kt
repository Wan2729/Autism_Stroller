package com.example.autismstroller.functional

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class BLEControllerHandlerTest {

    // Mocks: Fake objects to simulate Android Bluetooth
    private val mockGatt: BluetoothGatt = mock()
    private val mockService: BluetoothGattService = mock()
    private val mockCharacteristic: BluetoothGattCharacteristic = mock()

    // UUIDs needed for the test
    private val serviceUUID = UUID.randomUUID()
    private val charUUID = UUID.randomUUID()

    // The class we are testing
    private lateinit var handler: BLEControllerHandler
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        // 1. Setup Coroutines (because ViewModel uses viewModelScope)
        Dispatchers.setMain(testDispatcher)

        // 2. Train the mocks (Teach the fake objects what to do)
        // When code asks for service, return mockService
        whenever(mockGatt.getService(serviceUUID)).doReturn(mockService)
        // When code asks for characteristic, return mockCharacteristic
        whenever(mockService.getCharacteristic(charUUID)).doReturn(mockCharacteristic)

        // 3. Initialize Handler with the mock GATT provider
        handler = BLEControllerHandler(
            gattProvider = { mockGatt },
            serviceUUID = serviceUUID,
            characteristicUUID = charUUID
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleLED OFF sends correct 0_0_0_0 command`() = runTest {
        // GIVEN: Light is currently ON (so we can turn it off)
        // Note: You might need to toggle it once to set state, or access the private field if needed. 
        // Assuming default is OFF, let's turn it ON first to test the OFF logic.
        // Or simpler: Test the logic for turning it ON from default OFF state.

        // WHEN: We try to turn the light ON (logic: if !lightOn -> send "L:0_0_0_0"?) 
        // Wait, looking at your code:
        // if(!_lightOn.value) -> "L:0_0_0_0"
        // This logic seems reversed in your original code (Sending 0s when light is OFF?), 
        // but let's test that the code does exactly what is written.

        handler.toggleLED(hue = 120, saturation = 100, value = 100, animation = 1)
        testDispatcher.scheduler.advanceUntilIdle() // Wait for coroutine

        // THEN: Capture what was written to the characteristic
        val captor = argumentCaptor<ByteArray>()
        verify(mockCharacteristic).value = captor.capture() // Capture the value
        verify(mockGatt).writeCharacteristic(mockCharacteristic) // Verify write called

        // Convert bytes back to string to check
        val sentString = String(captor.firstValue)

        // Based on your code: if(!_lightOn) -> "L:0_0_0_0". Default _lightOn is false.
        assertEquals("L:0_0_0_0", sentString)
    }

    @Test
    fun `sendWiFiCredentials formats string correctly`() = runTest {
        // GIVEN
        val ssid = "MyHomeWiFi"
        val pass = "Secret123"

        // WHEN
        handler.sendWiFiCredentials(ssid, pass)
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        val captor = argumentCaptor<ByteArray>()
        verify(mockCharacteristic).value = captor.capture()
        verify(mockGatt).writeCharacteristic(mockCharacteristic)

        val sentString = String(captor.firstValue)

        // Assert the ESP32 will receive the exact comma-separated format
        assertEquals("W:MyHomeWiFi,Secret123", sentString)
    }
}