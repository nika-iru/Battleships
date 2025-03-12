package com.fuentes.battleships.modules.game.multiplayer.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fuentes.battleships.modules.auth.ui.AuthViewModel
import com.fuentes.battleships.modules.game.GameLogic
import com.fuentes.battleships.modules.game.multiplayer.data.GameSession
import com.fuentes.battleships.modules.game.multiplayer.data.GameViewModel
import com.fuentes.battleships.modules.game.singleplayer.data.Cell
import com.fuentes.battleships.modules.game.singleplayer.data.GameState
import com.fuentes.battleships.modules.game.singleplayer.ui.BattleshipGrid
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay

@Composable
fun MultiplayerScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    navController: NavController,
    sessionId: String
) {
    val gameViewModel: GameViewModel = viewModel()
    var gameSession by remember { mutableStateOf(GameSession()) }
    val gameLogic = GameLogic()

    // Listen for Firestore updates
    LaunchedEffect(sessionId) {
            gameViewModel.initializeGame(sessionId)
            Log.d("MultiplayerScreen", "Initializing game with sessionId: $sessionId")
    }

    // Observe changes to the game session
    LaunchedEffect(gameViewModel.gameSession) {
        gameSession = gameViewModel.gameSession.value
    }

    // Main UI
    Surface(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // Game header
            Text(
                text = when (gameSession.phase) {
                    0 -> "Player ${if (gameSession.isPlayer1Turn) 1 else 2} Place Your Ship"
                    1 -> "Player ${if (gameSession.isPlayer1Turn) 1 else 2}'s Attack Turn"
                    else -> "Game Over! Player ${if (gameSession.isPlayer1Turn) 1 else 2} Wins!"
                },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Ship placement controls (only visible during placement phase)
            if (gameSession.phase == 0) {
                Button(
                    onClick = {
                        // Toggle ship orientation
                        gameViewModel.updateGameSession(
                            gameSession.copy(isHorizontal = !gameSession.isHorizontal)
                        )
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(if (gameSession.isHorizontal) "Horizontal Placement" else "Vertical Placement")
                }
            }

            // Game Board
            BattleshipGrid(
                board = if (gameSession.phase == 1) {
                    // Show the opponent's board during the attack phase
                    gameLogic.createBoardForAttackingMultiplayer(gameSession)
                } else {
                    // Show the player's own board during the placement phase
                    gameLogic.createBoardWithShips(
                        if (gameSession.isPlayer1Turn) gameSession.player1Ships else gameSession.player2Ships,
                        emptyList(), // No hits or misses during placement
                        emptyList()
                    )
                },
                enabled = when (gameSession.phase) {
                    0 -> true // Enable during placement phase
                    1 -> true // Enable during attack phase
                    else -> false // Disable during game over
                },
                onCellClick = { x, y ->
                    when (gameSession.phase) {
                        0 -> {
                            // Handle ship placement
                            gameViewModel.handlePlacement(x, y, gameSession) { updatedSession ->
                                gameSession = updatedSession
                            }
                        }

                        1 -> {
                            // Handle attack
                            gameViewModel.handleAttack(x, y, gameSession) { updatedSession ->
                                gameSession = updatedSession
                            }
                        }

                        2 -> {
                            // Game over, do nothing
                        }
                    }
                }
            )

            // Reset button (visible during game over)
            if (gameSession.phase == 2) {
                Button(
                    onClick = {
                        // Reset the game session
                        gameViewModel.resetGameSession(gameSession.sessionId!!)
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Play Again")
                }
            }
        }
    }
}

