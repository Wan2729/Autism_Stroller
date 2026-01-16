package com.example.autismstroller.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.autismstroller.R
import com.example.autismstroller.models.Message
import com.example.autismstroller.models.MessageStatus
import com.example.autismstroller.utilities.AppColors
import com.example.autismstroller.utilities.ImageManipulator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class) // Added ExperimentalLayoutApi
@Composable
fun ChatScreen(navController: NavController, roomId: String, userId: String) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var friendId by remember { mutableStateOf<String?>(null) }
    val loadingProfilePicture = ImageManipulator.vectorToBitmap(context, R.drawable.icon_loading_pfp)
    var friendPfp by remember { mutableStateOf<Bitmap>(loadingProfilePicture) }
    var friendName by remember { mutableStateOf("Loading...") }

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Detect keyboard state
    val isKeyboardOpen = WindowInsets.isImeVisible

    // Fetch Friend Info
    LaunchedEffect(Unit) {
        // ... (Your existing code for fetching friend info) ...
        // (Keeping this part short for brevity, paste your existing logic here)
        val response1 = db.collection("chatRooms").document(roomId).get().await()
        val participants = response1.get("participantsId") as? List<String> ?: emptyList()
        val idFound = if (participants.firstOrNull() == userId) participants.getOrNull(1) else participants.firstOrNull()
        friendId = idFound

        if (idFound != null) {
            val response2 = db.collection("users").document(idFound).get().await()
            val friendPfpBase64 = response2.getString("profilePicture")
            friendPfp = ImageManipulator.decodeBase64ToBitmap(friendPfpBase64) ?: loadingProfilePicture
            friendName = response2.getString("name") ?: "Unknown"
        }
    }

    // Realtime Messages Listener
    DisposableEffect(roomId) {
        val listener = db.collection("messages")
            .whereEqualTo("roomId", roomId)
            .orderBy("timestamp")
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val newMessages = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                    messages = newMessages
                }
            }
        onDispose { listener.remove() }
    }

    LaunchedEffect(messages.size, isKeyboardOpen) {
        if (messages.isNotEmpty()) {
            // scrollToItem is instant (good for first load), animate is smooth.
            // We use animate here for better UX.
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Scroll to bottom when KEYBOARD opens
    LaunchedEffect(isKeyboardOpen) {
        if (isKeyboardOpen && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                name = friendName,
                profilePic = friendPfp,
                onBack = { navController.popBackStack() },
                onProfileClick = {
                    if (friendId != null) {
                        navController.navigate("friendProfileScreen/$friendId")
                    }
                }
            )
        },
        // We REMOVE bottomBar from here to handle IME padding manually for smoother effect
    ) { paddingValues ->

        // Root Column handles the layout structure
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FB))
                .padding(paddingValues) // Padding from TopBar
                .imePadding() // This pushes the input bar up when keyboard opens
        ) {
            // 1. The Chat List (Takes all available space)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f) // Fill space between TopBar and Input
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(messages) { index, item ->
                    val isMe = item.senderId == userId
                    MessageBubble(message = item, isMe = isMe)
                }
            }

            // 2. The Input Bar (Stays at the bottom of the Column)
            ChatInputBar(
                value = messageText,
                onValueChange = { messageText = it },
                onSend = {
                    val cleanText = messageText.trim()
                    if (cleanText.isNotEmpty()) {
                        sendMessage(db, roomId, userId, messageText.trim()) {
                            messageText = ""
                        }
                    }
                }
            )
        }
    }
}

// --- COMPONENTS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    name: String,
    profilePic: Bitmap,
    onBack: () -> Unit,
    onProfileClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onProfileClick() } // Make it clickable
                    .padding(8.dp) // Add touch target padding
            ) {
                Image(
                    painter = BitmapPainter(profilePic.asImageBitmap()),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    // Optional: Online status
//                    Text(text = "Online", fontSize = 12.sp, color = Color.Gray)
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
        modifier = Modifier.shadow(4.dp)
    )
}

@Composable
fun MessageBubble(message: Message, isMe: Boolean) {
    val bubbleColor = if (isMe) AppColors.SoftBlue else Color.White
    val textColor = if (isMe) Color.White else Color.Black
    val alignment = if (isMe) Alignment.End else Alignment.Start

    val displayText = if (message.isEncrypted) {
        com.example.autismstroller.utilities.EncryptionManager.decrypt(message.text, message.roomId)
    } else {
        message.text // Old messages show as is
    }

    // Asymmetrical corners to create a "tail" effect
    val shape = if (isMe) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 2.dp, bottomEnd = 18.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = displayText,
                    color = textColor,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(message.timestamp),
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Input Field
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Type a message...", fontSize = 14.sp) },
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF0F0F0)), // Light gray fill
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF0F0F0),
                unfocusedContainerColor = Color(0xFFF0F0F0),
                focusedIndicatorColor = Color.Transparent, // Hides the underline
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            maxLines = 3
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Send Button
        IconButton(
            onClick = onSend,
            modifier = Modifier
                .size(48.dp)
                .background(AppColors.SoftBlue, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "Send",
                tint = Color.White,
                modifier = Modifier.padding(start = 2.dp) // Optical centering
            )
        }
    }
}

// Utility to format timestamp
fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

fun sendMessage(
    db: FirebaseFirestore,
    roomId: String,
    senderId: String,
    text: String,
    onComplete: () -> Unit
) {
    val messageId = db.collection("messages").document().id

    // 1. Encrypt the text
    val encryptedText = com.example.autismstroller.utilities.EncryptionManager.encrypt(text, roomId)

    val message = Message(
        id = messageId,
        roomId = roomId,
        senderId = senderId,
        text = encryptedText, // Send CIPHER text
        timestamp = Timestamp.now(),
        status = MessageStatus.SENT,
        isEncrypted = true // Mark as encrypted
    )

    db.collection("messages").document(messageId)
        .set(message)
        .addOnSuccessListener {
            // Update chat room's last message (Also Encrypted!)
            db.collection("chatRooms").document(roomId)
                .set(
                    mapOf(
                        "lastMessage" to encryptedText, // Store encrypted preview
                        "lastUpdate" to Timestamp.now(),
                        "lastSenderId" to senderId,
                        "lastMessageIsEncrypted" to true
                    ),
                    SetOptions.merge()
                )
                .addOnSuccessListener {
                    onComplete()
                }
                .addOnFailureListener { error ->
                    Log.e("Firestore", "Failed to update lastMessage", error)
                }
        }
}