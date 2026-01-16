package com.example.autismstroller.functional

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MusicPlayerTest {

    // Mocks
    private val mockContext: Context = mock()
    private val mockMediaPlayer: MediaPlayer = mock()
    private val mockAttributes: AudioAttributes = mock()

    private lateinit var musicPlayer: MusicPlayer

    // Variable to capture the listener so we can trigger it manually
    private var capturedPreparedListener: MediaPlayer.OnPreparedListener? = null

    @Before
    fun setup() {
        // 1. Train the mock to capture the listener
        // When code calls setOnPreparedListener, save the argument to our variable
        doAnswer { invocation ->
            capturedPreparedListener = invocation.arguments[0] as MediaPlayer.OnPreparedListener
            null
        }.whenever(mockMediaPlayer).setOnPreparedListener(any())

        // 2. Train isPlaying to avoid NPEs
        whenever(mockMediaPlayer.isPlaying).thenReturn(true)

        // 3. Initialize MusicPlayer injecting our Mocks
        musicPlayer = MusicPlayer(
            context = mockContext,
            playerFactory = { mockMediaPlayer }, // Inject Mock Player
            attributesProvider = { mockAttributes } // Inject Mock Attributes
        )
    }

    @Test
    fun `stop stops and releases player`() {
        // GIVEN: We play a song
        musicPlayer.playSong("http://dummy.url")

        // Simulate the song actually starting (Trigger the listener we captured)
        capturedPreparedListener?.onPrepared(mockMediaPlayer)

        // WHEN: We call stop
        musicPlayer.stop()

        // THEN: Verify the Android methods were called on the mock
        verify(mockMediaPlayer).stop()
        verify(mockMediaPlayer).release()

        // Assert state is updated
        assertFalse(musicPlayer.isPlaying.value)
    }
}