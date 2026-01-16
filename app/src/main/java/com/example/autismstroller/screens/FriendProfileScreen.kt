package com.example.autismstroller.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.autismstroller.functional.AchievementHandler
import com.example.autismstroller.functional.AchievementHandlerFactory
import com.example.autismstroller.functional.UserHandler
import com.example.autismstroller.functional.UserState
import com.example.autismstroller.reusables.*
import com.example.autismstroller.utilities.ImageManipulator
import com.google.firebase.auth.FirebaseAuth
import com.example.autismstroller.utilities.AppColors
import kotlinx.coroutines.launch

@Composable
fun FriendProfileScreen(navController: NavController, friendId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val myUid = currentUser?.uid ?: ""

    val userHandler: UserHandler = viewModel()
    val userState by userHandler.userstate.collectAsState()
    val achievementHandler: AchievementHandler = viewModel(
        factory = AchievementHandlerFactory(userHandler)
    )

    // 1. Fetch Friend's Profile (for display)
    LaunchedEffect(friendId) {
        userHandler.getUserDetails(friendId)
    }

    // 2. Fetch MY Relationship Status (for the button)
    var isFollowing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (myUid.isNotEmpty()) {
            userHandler.checkIsFriend(myUid, friendId) { isFriend ->
                isFollowing = isFriend
            }
        }
    }

    var isAddingFriend by remember { mutableStateOf(false) }

    val userDetails = (userState as? UserState.UserDetails)?.userDetail.takeIf { it?.uid == friendId }

    if (userDetails == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val friendBitmap = remember(userDetails.profilePicture) {
            ImageManipulator.decodeBase64ToBitmap(userDetails.profilePicture)
        }

        // REMOVED: val isAlreadyFriend = userDetails.friends.contains(myUid)
        // We use 'isFollowing' instead.

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Friend Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold)

                if (myUid.isNotEmpty() && myUid != friendId) {
                    Button(
                        onClick = {
                            if (!isFollowing) {
                                isAddingFriend = true
                                userHandler.addFriend(
                                    myUid,
                                    friendId,
                                    onSuccess = { _ ->
                                        scope.launch {
                                            // A. Unlock Achievements
                                            val unlocked = achievementHandler.checkAndUnlockByStat(myUid, "friends")
                                            if (unlocked.isNotEmpty()) {
                                                Toast.makeText(context, "Achievement: ${unlocked.first().title}!", Toast.LENGTH_LONG).show()
                                            }

                                            // B. UPDATE UI IMMEDIATELY
                                            Toast.makeText(context, "Friend Added!", Toast.LENGTH_SHORT).show()

                                            // FORCE THE STATE CHANGE HERE
                                            isFollowing = true
                                            isAddingFriend = false
                                        }
                                    }
                                )
                            }
                        },
                        // Use isFollowing to determine color
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) Color.Gray else AppColors.SoftBlue
                        ),
                        enabled = !isFollowing && !isAddingFriend,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (isAddingFriend) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            // Use isFollowing to determine text
                            Text(
                                text = if (isFollowing) "Following" else "Follow", // Changed text to match logic better
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // ── Profile Card ──
            ProfileCard(
                context = context,
                isEditing = false,
                name = userDetails.name ?: "Unknown",
                onNameChange = {},
                level = userDetails.level,
                displayBitmap = friendBitmap,
                onAvatarClick = {}
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Description ──
            InfoBox(
                title = "Description",
                value = userDetails.description ?: "No description provided.",
                isEditing = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Stats Row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoBox(
                    title = "Age",
                    value = userDetails.age.toString(),
                    isEditing = false,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                GenderBox(
                    gender = if (userDetails.genderMale) "Male" else "Female",
                    isEditing = false,
                    onGenderChange = {},
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FriendsBox(
                    count = userDetails.friends.size,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { navController.navigate("friendListScreen/${friendId}") }
                )
            }
        }
    }
}