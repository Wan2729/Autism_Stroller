package com.example.autismstroller.reusables

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.autismstroller.models.Song
import com.example.autismstroller.utilities.AppColors

@Composable
fun SongPickerDialog(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onSongSelected: (Song) -> Unit,
    onUploadRequest: (String, Uri, Boolean) -> Unit
) {
    var showUploadUI by remember { mutableStateOf(false) }

    // Upload State
    var newSongName by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var isPublicUpload by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f) // Take up 85% of screen height
        ) {
            Column(Modifier.padding(16.dp)) {
                // Header
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if(showUploadUI) "Upload Song" else "Select Song",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                Divider(Modifier.padding(vertical = 12.dp))

                if (showUploadUI) {
                    // -- UPLOAD FORM --
                    Column(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = newSongName,
                            onValueChange = { newSongName = it },
                            label = { Text("Song Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = { launcher.launch("audio/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.SoftBlue)
                        ) {
                            Text(text = if (selectedFileUri == null) "Select MP3 File" else "File Selected")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isPublicUpload, onCheckedChange = { isPublicUpload = it })
                            Text("Make Public (Others can see)")
                        }

                        Spacer(Modifier.weight(1f))

                        Button(
                            onClick = {
                                if (newSongName.isNotEmpty() && selectedFileUri != null) {
                                    onUploadRequest(newSongName, selectedFileUri!!, isPublicUpload)
                                    showUploadUI = false // Go back to list
                                    Toast.makeText(context, "Uploading...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.SoftYellow)
                        ) {
                            Text("Upload Now")
                        }
                        TextButton(
                            onClick = { showUploadUI = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                } else {
                    // -- SONG LIST --
                    LazyColumn(Modifier.weight(1f)) {
                        items(songs) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                    .clickable { onSongSelected(song) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = song.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Text(
                                        text = "Plays: ${song.listenCount} • ${if (song.isPublic) "Public" else "Private"}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AppColors.SoftBlue)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { showUploadUI = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.SoftBlue)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Upload New Song")
                    }
                }
            }
        }
    }
}