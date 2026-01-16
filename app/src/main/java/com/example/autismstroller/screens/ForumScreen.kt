package com.example.autismstroller.screens

import android.content.Context
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.autismstroller.R
import com.example.autismstroller.functional.AchievementHandler
import com.example.autismstroller.functional.AchievementHandlerFactory
import com.example.autismstroller.functional.ForumHandler
import com.example.autismstroller.functional.ForumState
import com.example.autismstroller.functional.UserHandler
import com.example.autismstroller.models.Achievement
import com.example.autismstroller.models.Forum
import com.example.autismstroller.models.UserStats
import com.example.autismstroller.reusables.figmaDropShadow
import com.example.autismstroller.utilities.AppColors
import com.example.autismstroller.utilities.ImageManipulator
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumScreen(navController: NavController, forumHandler: ForumHandler) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val userHandler: UserHandler = viewModel()

    // -- Achievement taking effect
    val achievementHandler: AchievementHandler = viewModel(
        factory = AchievementHandlerFactory(UserHandler())
    )
    val stat = listOf("timeSpendOnForum")

    val myForumState by forumHandler.forumstate.collectAsState()
    val forumList: MutableList<Forum> = remember {
        mutableStateListOf()
    }
    var screenSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        forumHandler.getForum()
        while (true) {
            delay(1000) // 1 second
            screenSeconds++
            if (screenSeconds == 60) {
                userHandler.increment(user?.uid?:"INVALID", stat.get(0), 60)
                val newAchievements = achievementHandler.checkAndUnlockByStat(user?.uid ?: "INVALID",   stat.get(0))
                if(newAchievements.isNotEmpty()){
                    Toast.makeText(context, "New Achievement Unlocked !", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    when(myForumState){
        is ForumState.Success -> {
            val forums = (myForumState as ForumState.Success).forums
            if (forumList != forums) {
                forumList.clear()
                forumList.addAll(forums)
            }
        }
        is ForumState.Error ->{
            val error = (myForumState as ForumState.Error).message
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            forumHandler.clearError()
        }
        else -> Unit
    }

    var showDialog by remember { mutableStateOf(false) }
    var forumTopic by remember { mutableStateOf(TextFieldValue("")) }
    var forumText by remember { mutableStateOf(TextFieldValue("")) }

    Column (
        modifier = Modifier.padding(12.dp)

    ){
        Text(text = "Forum", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        if (forumList.isEmpty()) {
            Text(text = "No forum available", fontSize = 16.sp, color = Color.Gray)
        }
        LazyColumn(
            modifier = Modifier.padding(0.dp,12.dp,0.dp,48.dp),
            content = {
            itemsIndexed(forumList, itemContent = { index, item ->
                ForumItem(
                    forum = item,
                    context,
                    onLike = {
                        Toast.makeText(context, "Liked a post", Toast.LENGTH_SHORT).show()
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

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Yellow, shape = RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "Create a New Forum", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = forumTopic,
                        onValueChange = { forumTopic = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Topic") }, // Hint text
                        singleLine = true,
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = forumText,
                        onValueChange = { forumText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        placeholder = { Text("What's on your mind?") }, // Hint text
                        singleLine = false,
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = {
                                showDialog = false
                                forumTopic = TextFieldValue("")
                                forumText = TextFieldValue("")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(text = "Cancel")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (forumTopic.text.isEmpty() || forumText.text.isEmpty()){
                                    Toast.makeText(context, "Please enter the fields", Toast.LENGTH_SHORT).show()
                                }
                                else {
                                    forumHandler.create(
                                        topic = forumTopic.text,
                                        text = forumText.text.trim()
                                    )
                                    Log.d("AfterSubmit",forumList.toString())
                                    showDialog = false
                                    forumTopic = TextFieldValue("")
                                    forumText = TextFieldValue("")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Blue,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(text = "Submit")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ForumItem(
    forum: Forum,
    context: Context,
    onLike: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    var username by remember {
        mutableStateOf("Loading...")
    }
    var profilePicture by remember {
        mutableStateOf(ImageManipulator.vectorToBitmap(context, R.drawable.icon_defaultprofile1))
    }
    var loading by remember{
        mutableStateOf(true)
    }

    LaunchedEffect(forum.sender) {
        val firestore = FirebaseFirestore.getInstance()
        try {
            val snapshot = firestore.collection("users").document(forum.sender).get().await()
            val name = snapshot.getString("name")
            val imageBase64 = snapshot.getString("profilePicture")
            if (!imageBase64.isNullOrEmpty()) {
                val bitmap = ImageManipulator.decodeBase64ToBitmap(imageBase64)
                profilePicture = bitmap?: profilePicture
            }
            username = name ?: "Unknown"
            loading = false
        } catch (e: Exception) {
            Log.e("ForumItem", "Error fetching username", e)
            username = "Unknown"
        }
    }

    Column(
        modifier = Modifier
            .padding(4.dp)
            .figmaDropShadow(
                color = Color.Gray.copy(alpha = 0.3f),
                offsetY = 1.dp,
                blurRadius = 4.dp,
                borderRadius = 10.dp
            )
            .clip(RoundedCornerShape(10.dp))
            .background(color = Color.White)
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp)) // Ripple shape
                .clickable { onProfileClick(forum.sender) } // Pass sender ID
                .padding(4.dp) // Touch padding
        ) {
            Image(
                painter = BitmapPainter(profilePicture.asImageBitmap()) ,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(40.dp)
                    .padding(8.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Text(text = username,fontSize = 12.sp)
        }
        Text(text = forum.topic, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Row {
            Text(
                text = forum.text,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                modifier = Modifier.weight(9f)
            )
            Icon(
                painter = painterResource(id = R.drawable.icon_like_false),
                contentDescription = "Like Button",
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onLike),
                tint = Color.Unspecified
            )
        }
    }
}