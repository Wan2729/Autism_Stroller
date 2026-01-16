package com.example.autismstroller.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.autismstroller.R
import com.example.autismstroller.functional.ForumHandler
import com.example.autismstroller.functional.ForumState
import com.example.autismstroller.functional.UserHandler
import com.example.autismstroller.functional.UserState
import com.example.autismstroller.models.Forum
import com.example.autismstroller.models.User
import com.example.autismstroller.reusables.FriendsBox
import com.example.autismstroller.reusables.figmaDropShadow
import com.example.autismstroller.utilities.ImageManipulator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumFriendScreen(navController: NavController, forumHandler: ForumHandler) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance()

    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var searchActive by remember { mutableStateOf(false) }
    val userHandler: UserHandler = viewModel()
    val userState by userHandler.userstate.collectAsState()
    val userDetails = remember(userState) { (userState as? UserState.UserDetails)?.userDetail }

    LaunchedEffect(user.uid){
        userHandler.getUserDetails(user.uid.orEmpty())
    }

    val myForumState by forumHandler.forumstate.collectAsState()
    val forumList: MutableList<Forum> = remember { mutableStateListOf() }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        forumHandler.getForum(user.uid.toString())
    }
    LaunchedEffect(myForumState) {
        when(myForumState){
            is ForumState.Success -> {
                val forums = (myForumState as ForumState.Success).forums
                // Only update if actually different to save resources
                if (forumList.size != forums.size || forumList != forums) {
                    forumList.clear()
                    forumList.addAll(forums)
                }
                loading = false
                Log.d("ForumFriendScreen", "Fetch Success") // This will now print only once
            }
            is ForumState.Error -> {
                val error = (myForumState as ForumState.Error).message
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                forumHandler.clearError()
                loading = false
            }
            else -> {}
        }
    }

    if (myForumState is ForumState.Error) {
        val error = (myForumState as ForumState.Error).message
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        forumHandler.clearError()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Text(text = "Forum > Friends", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            FriendsBox(
                count = userDetails?.friends?.size ?: 0,
                modifier = Modifier.clickable { navController.navigate("friendListScreen/${user.uid}") }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        SearchBar(
            query = query,
            onQueryChange = {
                query = it
                searchActive = true
                if (query.isNotEmpty()) {
                    searchFriends(db, query) { results -> searchResults = results }
                } else {
                    searchResults = emptyList()
                }
            },
            onSearch = {},
            active = searchActive,
            onActiveChange = { searchActive = it },
            placeholder = { Text("Add friend...") }
        ) {
            if (searchResults.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.padding(0.dp, 12.dp, 0.dp, 48.dp)
                ) {
                    itemsIndexed(searchResults) { index, item ->
                        val pfpBase64 = item.profilePicture
                        val pfpBitmap = ImageManipulator.decodeBase64ToBitmap(pfpBase64)?:ImageManipulator.vectorToBitmap(context,R.drawable.icon_defaultprofile1)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .figmaDropShadow(
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    offsetY = 1.dp,
                                    blurRadius = 4.dp,
                                    borderRadius = 10.dp
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .padding(8.dp)
                                .clickable { navController.navigate("friendProfileScreen/${item.uid}") },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ){
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = BitmapPainter(pfpBitmap.asImageBitmap()),
                                    contentDescription = "Friend list profile picture",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = item.name, fontWeight = FontWeight.Bold)
                                    Text(text = "Level: ${item.level}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            } else {
                Text(text = "No friends found", modifier = Modifier.padding(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if(loading){
            Text("Loading forums...", modifier = Modifier.padding(top = 12.dp))
        } else {
            val forums = forumList
            if (forums.isEmpty()) {
                Text("No forums from friends", modifier = Modifier.padding(top = 12.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.padding(0.dp, 12.dp, 0.dp, 48.dp),
                    content = {
                        itemsIndexed(forums, itemContent = { index, item ->
                            ForumItem(
                                forum = item,
                                context,
                                onLike = {
                                    Toast.makeText(context, "Liked a post", Toast.LENGTH_SHORT)
                                        .show()
                                },
                                onProfileClick = { authorId ->
                                    // Don't navigate if clicking own profile, or do (up to you)
                                    if (authorId != user?.uid) {
                                        navController.navigate("friendProfileScreen/$authorId")
                                    } else {
                                        // Optional: Navigate to "My Profile" tab or do nothing
                                        Toast.makeText(context, "That's you!", Toast.LENGTH_SHORT).show()
                                    }
                                }

                            )
                        })
                    })
            }
        }

//        when (myForumState) {
//            is ForumState.Loading -> {
//                Text("Loading forums...", modifier = Modifier.padding(top = 12.dp))
//            }
//
//            is ForumState.Success -> {
//                val forums = (myForumState as ForumState.Success).forums
//                if (forums.isEmpty()) {
//                    Text("No forums from friends", modifier = Modifier.padding(top = 12.dp))
//                } else {
//                    LazyColumn(
//                        modifier = Modifier.padding(0.dp, 12.dp, 0.dp, 48.dp),
//                        content = {
//                            itemsIndexed(forums, itemContent = { index, item ->
//                                ForumItem(forum = item, context, onLike = {
//                                    Toast.makeText(context, "Liked a post", Toast.LENGTH_SHORT)
//                                        .show()
//                                })
//                            })
//                        })
//                }
//            }
//            else -> Unit // Already handled error above
//        }
    }
}

fun searchFriends(db: FirebaseFirestore, query: String, onResult: (List<User>) -> Unit) {
    db.collection("users")
        .whereGreaterThanOrEqualTo("name", query)
        .whereLessThanOrEqualTo("name", query + "\uf8ff") // Helps with partial matching
        .get()
        .addOnSuccessListener { result ->
            val users = result.documents.mapNotNull { it.toObject(User::class.java) }
            onResult(users)
        }
        .addOnFailureListener {
            onResult(emptyList())
        }
}