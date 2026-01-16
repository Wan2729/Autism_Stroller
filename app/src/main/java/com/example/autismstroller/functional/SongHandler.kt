package com.example.autismstroller.functional

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autismstroller.models.Song
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class SongHandler(
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchSongs()
    }

    /**
     * Uploads the file to Supabase, generates the public URL,
     * and saves the metadata to Firestore matching your Song model.
     */
    fun uploadSong(context: Context, name: String, fileUri: Uri, isPublic: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUser = auth.currentUser
            val uid = currentUser?.uid ?: return@launch

            // Generate a random ID for the song document
            val songId = UUID.randomUUID().toString()

            try {
                // 1. Read file bytes from the URI
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
                } ?: throw Exception("Unable to read file")

                // 2. Upload to Supabase Storage (Bucket: 'music')
                val bucket = supabaseClient.storage.from("music")
                val path = "$uid/$songId.mp3"
                bucket.upload(path, bytes)

                // 3. Get the Public URL
                val publicUrl = bucket.publicUrl(path)

                // 4. Create the Song Object (Matches your Model exactly)
                val newSong = Song(
                    id = songId,
                    name = name,
                    url = publicUrl,
                    listenCount = 0,                 // Default start
                    timeCreated = System.currentTimeMillis(),
                    isPublic = isPublic,
                    ownerId = uid
                )

                // 5. Save to Firestore
                db.collection("songs").document(songId).set(newSong).await()

                Log.d("MusicHandler", "Song saved: $name")

                // 6. Refresh the list
                fetchSongs()

            } catch (e: Exception) {
                Log.e("MusicHandler", "Upload failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUser = auth.currentUser?.uid ?: ""

            try {
                // Fetch Public Songs
                val publicSnapshot = db.collection("songs")
                    .whereEqualTo("isPublic", true)
                    .get().await()
                val publicSongs = publicSnapshot.toObjects(Song::class.java)

                // Fetch Private Songs (Owned by me)
                var myPrivateSongs: List<Song> = emptyList()
                if (currentUser.isNotEmpty()) {
                    val privateSnapshot = db.collection("songs")
                        .whereEqualTo("ownerId", currentUser)
                        .whereEqualTo("isPublic", false)
                        .get().await()
                    myPrivateSongs = privateSnapshot.toObjects(Song::class.java)
                }

                // Combine and Sort
                // Priority: High Listen Count -> Newest Time
                val combined = (publicSongs + myPrivateSongs)
                    .distinctBy { it.id } // Avoid duplicates if any logic overlaps
                    .sortedWith(
                        compareByDescending<Song> { it.listenCount }
                            .thenByDescending { it.timeCreated }
                    )

                _songs.value = combined

            } catch (e: Exception) {
                Log.e("MusicHandler", "Error fetching songs", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun recordListen(song: Song) {
        viewModelScope.launch {
            try {
                val newCount = song.listenCount + 1
                db.collection("songs").document(song.id)
                    .update("listenCount", newCount)
                    .await()
            } catch (e: Exception) {
                Log.e("MusicHandler", "Failed to update listen count", e)
            }
        }
    }
}