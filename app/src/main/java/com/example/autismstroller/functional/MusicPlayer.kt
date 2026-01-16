package com.example.autismstroller.functional

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicPlayer(
    private val context: Context,
    // Hook 1: Factory to create the player (Defaults to real Android MediaPlayer)
    private val playerFactory: () -> MediaPlayer = { MediaPlayer() },
    // Hook 2: Provider for Attributes (Defaults to null -> use real Builder)
    private val attributesProvider: (() -> AudioAttributes)? = null
) {

    private var mediaPlayer: MediaPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun playSong(url: String, isLooping: Boolean = false) {
        stop()

        try {
            // Use the factory (Tests will inject a mock here)
            mediaPlayer = playerFactory().apply {
                setDataSource(url)

                // Use provider if exists (Tests), else use real Builder
                val attributes = attributesProvider?.invoke() ?: AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

                setAudioAttributes(attributes)
                setLooping(isLooping)

                setOnPreparedListener {
                    start()
                    _isPlaying.value = true
                }

                setOnCompletionListener {
                    if (!isLooping) {
                        _isPlaying.value = false
                        release()
                        mediaPlayer = null
                    }
                }

                setOnErrorListener { _, _, _ ->
                    _isPlaying.value = false
                    stop()
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Error initializing player", e)
            _isPlaying.value = false
        }
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                try { it.stop() } catch (e: Exception) { e.printStackTrace() }
            }
            it.release()
        }
        mediaPlayer = null
        _isPlaying.value = false
    }
}