package com.example.autismstroller.screens

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.autismstroller.R
import com.example.autismstroller.functional.UserHandler
import com.example.autismstroller.functional.UserState
import com.example.autismstroller.models.ChatRoom
import com.example.autismstroller.models.User
import com.example.autismstroller.reusables.figmaDropShadow
import com.example.autismstroller.utilities.ImageManipulator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// Helper data class to combine Room info with the Other User's info
data class ChatSummary(
    val roomId: String,
    val peerUser: User,
    val lastMessage: String,
    val lastUpdate: Timestamp?,
    val lastSenderId: String,
    val isEncrypted: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendListScreen(navController: NavController, userId: String) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val userHandler: UserHandler = viewModel()
    val userState by userHandler.userstate.collectAsState()
    var userDetails by remember { mutableStateOf(User()) }

    // State for existing chats
    var chatList by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    var isLoadingChats by remember { mutableStateOf(true) }

    fun safeNavigateToChat(roomId: String) {
        // Check if we are ALREADY trying to go to this chat
        val currentRoute = navController.currentDestination?.route
        if (currentRoute?.contains(roomId) == true) return

        // Check if we are already on a chat screen generally
        if (currentRoute?.startsWith("chatScreen") == true) return

        navController.navigate("chatScreen/${roomId}/${userId}") {
            // "launchSingleTop" tells the app: "If I'm already going there, don't open it again"
            launchSingleTop = true
        }
    }

    LaunchedEffect(Unit) {
        userHandler.getUserDetails(userId)
    }

    // --- REALTIME LISTENER FOR CHATS ---
    // This fetches any room where "participantsId" contains my userId
    DisposableEffect(userId) {
        val query = db.collection("chatRooms")
            .whereArrayContains("participantsId", userId)
            .orderBy("lastUpdate", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("ChatList", "Listen failed", error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val fetchedChats = mutableListOf<ChatSummary>()
                val documents = snapshot.documents

                if(documents.isEmpty()) {
                    chatList = emptyList()
                    isLoadingChats = false
                    return@addSnapshotListener
                }

                var processedCount = 0

                // For every chat room, we need to fetch the OTHER person's profile
                for (doc in documents) {
                    val room = doc.toObject(ChatRoom::class.java)
                    val roomId = doc.id
                    val participants = room?.participantsId ?: emptyList()

                    // Identify the Peer (The ID that is NOT mine)
                    val peerId = participants.firstOrNull { it != userId } ?: userId // Fallback if talking to self

                    // Fetch Peer User Details
                    db.collection("users").document(peerId).get()
                        .addOnSuccessListener { userDoc ->
                            val peerUser = userDoc.toObject(User::class.java) ?: User(name = "Unknown")

                            fetchedChats.add(
                                ChatSummary(
                                    roomId = roomId,
                                    peerUser = peerUser,
                                    lastMessage = room?.lastMessage ?: "",
                                    lastUpdate = room?.lastUpdate,
                                    lastSenderId = room?.lastSenderId ?: "",
                                    isEncrypted = room?.lastMessageIsEncrypted ?: false
                                )
                            )

                            processedCount++
                            // Once all async fetches are done, update state
                            if (processedCount == documents.size) {
                                // Sort again locally to be safe (async might finish out of order)
                                chatList = fetchedChats.sortedByDescending { it.lastUpdate }
                                isLoadingChats = false
                            }
                        }
                }
            }
        }
        onDispose { listener.remove() }
    }

    when (userState) {
        is UserState.UserDetails -> userDetails =
            (userState as UserState.UserDetails).userDetail ?: User()
        is UserState.Error -> Toast.makeText(context, (userState as? UserState.Error)?.message, Toast.LENGTH_SHORT).show()
        else -> Unit
    }

    // --- SEARCH STATE ---
    var query by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(text = "Messages", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        SearchBar(
            query = query,
            onQueryChange = {
                query = it
                searchActive = true
                if (query.isNotEmpty()) {
                    // Search GLOBAL users to start a new chat
                    searchGlobalUsers(db, query) { results -> searchResults = results }
                } else {
                    searchResults = emptyList()
                }
            },
            onSearch = {},
            active = searchActive,
            onActiveChange = { searchActive = it },
            placeholder = {
                Text("Search people to chat...")
            }
        ) {
            // --- SEARCH RESULTS VIEW (Finding new people) ---
            LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
                itemsIndexed(searchResults) { index, item ->
                    val pfpBitmap = ImageManipulator.decodeBase64ToBitmap(item.profilePicture)
                        ?: ImageManipulator.vectorToBitmap(context, R.drawable.icon_defaultprofile1)

                    // Using a simpler card for search results
                    UserResultCard(user = item, profilePicture = pfpBitmap) {
                        createRoom(userId, item.uid, db) { roomId ->
                            safeNavigateToChat(roomId)
                        }
                    }
                }
            }
        }

        // --- MAIN CHAT LIST VIEW ---
        if(chatList.isEmpty() && !isLoadingChats){
            Spacer(modifier = Modifier.height(20.dp))
            Text("No messages yet. Use search to find friends!", color = Color.Gray)
        } else {
            LazyColumn(
                modifier = Modifier.padding(0.dp, 12.dp, 0.dp, 48.dp)
            ) {
                itemsIndexed(chatList) { index, chat ->
                    val pfpBitmap = ImageManipulator.decodeBase64ToBitmap(chat.peerUser.profilePicture)
                        ?: ImageManipulator.vectorToBitmap(context, R.drawable.icon_defaultprofile1)

                    ChatSummaryCard(
                        chatSummary = chat,
                        profilePicture = pfpBitmap,
                        currentUserId = userId,
                        onClick = {
                            safeNavigateToChat(chat.roomId)
                        }
                    )
                }
            }
        }
    }
}

// Helper function to search ALL users (like Instagram search)
fun searchGlobalUsers(db: FirebaseFirestore, query: String, onResult: (List<User>) -> Unit) {
    db.collection("users")
        .whereGreaterThanOrEqualTo("name", query)
        .whereLessThanOrEqualTo("name", query + "\uf8ff")
        .get()
        .addOnSuccessListener { result ->
            val users = result.documents.mapNotNull { it.toObject(User::class.java) }
            onResult(users)
        }
}

@Composable
fun ChatSummaryCard(
    chatSummary: ChatSummary,
    profilePicture: Bitmap,
    currentUserId: String,
    onClick: () -> Unit
){
    val isMeLast = chatSummary.lastSenderId == currentUserId
    val prefix = if(isMeLast) "You: " else ""
    val rawMessage = chatSummary.lastMessage
    val decryptedText = if (chatSummary.isEncrypted) {
        com.example.autismstroller.utilities.EncryptionManager.decrypt(rawMessage, chatSummary.roomId)
    } else {
        rawMessage
    }
    val messagePreview = if(rawMessage.isEmpty()) "Tap to start chatting" else decryptedText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .figmaDropShadow(
                color = Color.Gray.copy(alpha = 0.2f),
                offsetY = 2.dp,
                blurRadius = 6.dp,
                borderRadius = 12.dp
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ){
        // Profile Pic
        Image(
            painter = BitmapPainter(profilePicture.asImageBitmap()),
            contentDescription = "Profile Pic",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Name
            Text(
                text = chatSummary.peerUser.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Message Preview
            Text(
                text = "$prefix$messagePreview",
                style = MaterialTheme.typography.bodyMedium,
                color = if (chatSummary.lastMessage.isEmpty()) Color.Blue else Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontStyle = if (chatSummary.lastMessage.isEmpty()) FontStyle.Italic else FontStyle.Normal
            )
        }

        // Optional: Arrow or timestamp could go here
    }
}

@Composable
fun UserResultCard(
    user: User,
    profilePicture: Bitmap,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = BitmapPainter(profilePicture.asImageBitmap()),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = user.name, fontWeight = FontWeight.SemiBold)
    }
}

// Keep your existing createRoom logic, it is correct.
fun createRoom(
    userId: String,
    friendId: String,
    db: FirebaseFirestore,
    onResult: (String) -> Unit = {}
) {
    val participants = listOf(userId, friendId).sorted()
    val roomId = participants.joinToString("_")

    val roomRef = db.collection("chatRooms").document(roomId)

    roomRef.get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                onResult(roomId)
            } else {
                val newRoom = ChatRoom(
                    id = roomId,
                    participantsId = listOf(userId, friendId),
                    lastMessage = "",
                    lastSenderId = "",
                    lastUpdate = Timestamp.now()
                )
                roomRef.set(newRoom)
                    .addOnSuccessListener { onResult(roomId) }
            }
        }
}