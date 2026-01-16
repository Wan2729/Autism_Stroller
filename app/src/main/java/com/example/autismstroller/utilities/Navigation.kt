package com.example.autismstroller.utilities

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.autismstroller.functional.BLEHandler
import com.example.autismstroller.functional.ForumHandler
import com.example.autismstroller.screens.AchievementListScreen
import com.example.autismstroller.screens.AnalyticalScreen
import com.example.autismstroller.screens.ChatScreen
import com.example.autismstroller.screens.ChildDetailScreen
import com.example.autismstroller.screens.ChildListScreen
import com.example.autismstroller.screens.ForumFriendScreen
import com.example.autismstroller.screens.ForumScreen
import com.example.autismstroller.screens.FriendListScreen
import com.example.autismstroller.screens.FriendProfileScreen
import com.example.autismstroller.screens.HomeScreen
import com.example.autismstroller.screens.LoginScreen
import com.example.autismstroller.screens.ManageStrollerScreen
import com.example.autismstroller.screens.MyProfileScreen
import com.example.autismstroller.screens.RegisterScreen
import com.example.autismstroller.screens.SocialHolderScreen
import com.example.autismstroller.screens.StartScreen

@Composable
fun Navigation(bleHandler: BLEHandler) {
    val navController = rememberNavController()
    val forumHandler: ForumHandler = viewModel()

    NavHost(navController = navController, startDestination = "startScreen") {
        composable("startScreen") { StartScreen(navController) }
        composable("registerScreen") { RegisterScreen(navController) }
        composable("loginScreen") { LoginScreen(navController) }
        composable("homeScreen") { HomeScreen(navController) }
        composable("manageStrollerScreen") {
            ManageStrollerScreen(navController, bleHandler)
        }
        composable("myProfileScreen/{userId}"){backStackEntry ->
            val user = backStackEntry.arguments?.getString("userId") ?: ""
            MyProfileScreen(navController, user)
        }
        composable("friendProfileScreen/{friendId}"){backStackEntry ->
            val user = backStackEntry.arguments?.getString("friendId") ?: ""
            FriendProfileScreen(navController, user)
        }
        composable("friendListScreen/{userId}"){backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            FriendListScreen(navController, userId)
        }
        composable("chatScreen/{roomId}/{userId}") {backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ChatScreen(navController, roomId, userId)
        }
//        composable("bluetoothListScreen") {
//            BluetoothListScreen(navController, bleHandler)
//        }
        composable("newHomeScreen") { HomeScreen(navController) }
        composable("socialHolderScreen") { SocialHolderScreen(navController, forumHandler) }
        composable("achievementListScreen") { AchievementListScreen(navController) }
        composable("manageStrollerScreen") {
            ManageStrollerScreen(navController, bleHandler)
        }
        composable("childListScreen") { ChildListScreen(navController = navController) }
        composable("childDetailScreen/{childId}") { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId")
            ChildDetailScreen(navController, childId)
        }
        composable("analyticalScreen") { AnalyticalScreen(navController) }
    }
}