package com.example.autismstroller.functional

import android.bluetooth.BluetoothA2dp
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.autismstroller.ui.theme.AutismStrollerTheme
import com.example.autismstroller.utilities.Navigation
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

val supabase = createSupabaseClient(
    supabaseUrl = "https://bdqlurylipbhnapdhgky.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJkcWx1cnlsaXBiaG5hcGRoZ2t5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQ1MTY3OTksImV4cCI6MjA4MDA5Mjc5OX0.nypEwiVDXBkpS-cWhsoMY-gPsI7ZZ9LWKG1RfHQr0n0"
) {
    install(Postgrest)
    install(Storage)
}
val SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
val WRITE_UUID   = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
val NOTIFY_UUID  = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"

class MainActivity : ComponentActivity() {

    private lateinit var bleHandler: BLEHandler
    private lateinit var receiver: A2DPReciever

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleHandler = BLEHandler(this)
        receiver = A2DPReciever(bleHandler)

        registerReceiver(
            receiver,
            IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        )

        setContent {
            AutismStrollerTheme {
                Navigation(bleHandler)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}