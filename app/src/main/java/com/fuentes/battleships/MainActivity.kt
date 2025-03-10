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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fuentes.battleships.ui.theme.BattleshipsTheme
import com.fuentes.battleships.modules.auth.ui.AuthViewModel
import com.fuentes.battleships.modules.auth.ui.LoginScreen
import com.fuentes.battleships.modules.game.ui.GameScreen
import com.fuentes.battleships.modules.auth.ui.RegistrationScreen
import com.fuentes.battleships.modules.auth.ui.HomeScreen
import com.fuentes.battleships.modules.game.ui.SinglePlayerGameScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel by viewModels()

            BattleshipsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val authState = authViewModel.uiState.collectAsState()
                    val authEmail = authState.value.email

                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = if (authEmail.isNullOrEmpty()) "login" else "home"
                    ) {
                        composable("login") {
                            LoginScreen(authViewModel = authViewModel, navController = navController)
                        }
                        composable("registration") {
                            RegistrationScreen(authViewModel = authViewModel, navController = navController)
                        }
                        composable("game") {
                            GameScreen(authViewModel = authViewModel, navController = navController)
                        }
                        composable("home") {
                            HomeScreen(authViewModel = authViewModel, navController = navController)
                        }
                        composable("single") {
                            SinglePlayerGameScreen(authViewModel = authViewModel, navController = navController)
                        }
                    }
                }
            }
        }
    }
}