package com.fuentes.battleships

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fuentes.battleships.ui.theme.BattleshipsTheme
import com.fuentes.battleships.modules.auth.ui.AuthViewModel
import com.fuentes.battleships.modules.auth.ui.LoginScreen
import com.fuentes.battleships.modules.game.singleplayer.ui.GameScreen
import com.fuentes.battleships.modules.auth.ui.RegistrationScreen
import com.fuentes.battleships.modules.auth.ui.HomeScreen
import com.fuentes.battleships.modules.game.multiplayer.data.GameViewModel
import com.fuentes.battleships.modules.game.multiplayer.ui.HostScreen
import com.fuentes.battleships.modules.game.multiplayer.ui.OpponentScreen
import com.fuentes.battleships.modules.game.multiplayer.ui.SessionScreen
import com.fuentes.battleships.modules.game.singleplayer.ui.SinglePlayerGameScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel by viewModels()
            val gameViewModel: GameViewModel by viewModels()
            BattleshipsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val authState = authViewModel.uiState.collectAsState()
                    val authEmail = authState.value.email
                    val authUID = authState.value.userId

                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = if (authEmail.isNullOrEmpty()) "login" else "home"
                    ) {
                        composable("login") {
                            LoginScreen(
                                authViewModel = authViewModel,
                                navController = navController
                            )
                        }
                        composable("registration") {
                            RegistrationScreen(
                                authViewModel = authViewModel,
                                navController = navController
                            )
                        }
                        composable("game") {
                            GameScreen(authViewModel = authViewModel, navController = navController)
                        }
                        composable("home") {
                            HomeScreen(authViewModel = authViewModel, navController = navController)
                        }
                        composable("single") {
                            SinglePlayerGameScreen(
                                authViewModel = authViewModel,
                                navController = navController
                            )
                        }
                        composable("online") {
                            SessionScreen(
                                authViewModel = authViewModel,
                                navController = navController,
                                gameViewModel = gameViewModel
                            )
                        }

                        composable(
                            route = "game/{sessionId}",
                            arguments = listOf(navArgument("sessionId") {
                                type = NavType.StringType
                            })
                        ) { backStackEntry ->
                            // Retrieve the sessionId from the navigation arguments
                            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                            val currentUserId = authState.value.userId

                            // Fetch the game session details from the GameViewModel
                            LaunchedEffect(sessionId) {
                                gameViewModel.updateGameSession(sessionId)
                            }

                            // Collect the game session state
                            val gameSession by gameViewModel.gameSession.collectAsState()

                            // Check if the current user is the creator (player1) of the session
                            val isCreator = gameSession.player1Id == currentUserId

                            // Navigate to the appropriate screen based on whether the user is the creator
                            if (isCreator) {
                                HostScreen(
                                    authViewModel = authViewModel,
                                    navController = navController
                                )
                            } else {
                                OpponentScreen(
                                    authViewModel = authViewModel,
                                    navController = navController
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}