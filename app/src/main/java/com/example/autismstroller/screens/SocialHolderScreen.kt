package com.example.autismstroller.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.autismstroller.R
import com.example.autismstroller.functional.ForumHandler
import com.example.autismstroller.functional.ForumState
import com.example.autismstroller.models.Forum
import com.example.autismstroller.utilities.AppColors

@Composable
fun SocialHolderScreen(navController: NavController, forumHandler: ForumHandler) {
    val context = LocalContext.current
    val bottomNavController = rememberNavController()
    var showPostDialog by remember { mutableStateOf(false) }

    val myForumState by forumHandler.forumstate.collectAsState()
    val forumList: MutableList<Forum> = remember {
        mutableStateListOf()
    }

    LaunchedEffect(Unit) {
        forumHandler.getForum()
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

    if (showPostDialog) {
        PostForumDialog(
            forumHandler = forumHandler,
            forumList = forumList,
            onDismiss = { showPostDialog = false }
        )
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(bottomNavController, onAddPostClick = { showPostDialog = true })
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) { ForumScreen(navController, forumHandler) }
            composable(BottomNavItem.Explore.route) { ForumFriendScreen(navController, forumHandler) }
//            composable(BottomNavItem.AddPost.route) { AddPostScreen() }
//            composable(BottomNavItem.Profile.route) { MyProfileScreen(navController) }
        }
    }
}

@Composable
fun BottomNavigationBar(
    bottomNavController: NavController,
    onAddPostClick: () -> Unit = {}
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Explore,
        BottomNavItem.AddPost,
//        BottomNavItem.Profile
    )

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color.White,
        contentColor = Color.Black
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                icon = {
                    Icon(painterResource(id = item.icon), contentDescription = item.label)
                },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    if (item == BottomNavItem.AddPost) {
                        onAddPostClick()
                    } else if (!isSelected) {
                        bottomNavController.navigate(item.route) {
                            popUpTo(bottomNavController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostForumDialog(
    forumHandler: ForumHandler,
    forumList: List<Forum>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val myForumState by forumHandler.forumstate.collectAsState()

    var forumTopic by remember { mutableStateOf(TextFieldValue("")) }
    var forumText by remember { mutableStateOf(TextFieldValue("")) }

    Dialog(onDismissRequest = { onDismiss() }) {
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
                            onDismiss()
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
                                Log.d("AfterSubmit", forumList.toString())
                                onDismiss()
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


sealed class BottomNavItem(val route: String, val icon: Int, val label: String) {
    object Home : BottomNavItem("home", R.drawable.icon_loading_pfp, "Home")
    object Explore : BottomNavItem("explore", R.drawable.icon_loading_pfp, "Explore")
    object AddPost : BottomNavItem("add_post", R.drawable.icon_loading_pfp, "Post")
//    object Profile : BottomNavItem("profile", R.drawable.icon_loading_pfp, "Profile")
}
