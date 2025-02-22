package com.fuentes.battleships

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fuentes.battleships.ui.theme.BattleshipsTheme
import com.fuentes.battleships.models.game.data.Cell
import com.fuentes.battleships.models.auth.ui.AuthState
import com.fuentes.battleships.models.auth.ui.AuthViewModel
import com.fuentes.battleships.models.auth.ui.LoginScreen
import com.fuentes.battleships.models.game.ui.GameScreen
import com.fuentes.battleships.models.auth.ui.RegistrationScreen
import com.fuentes.battleships.models.auth.ui.HomeScreen

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
                            GameScreen(navController = navController)
                        }
                        composable("home") {
                            HomeScreen(authViewModel = authViewModel, navController = navController)
                        }
                    }
                }
            }
        }
    }
}