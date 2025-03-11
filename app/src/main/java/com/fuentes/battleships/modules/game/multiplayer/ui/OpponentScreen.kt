package com.fuentes.battleships.modules.game.multiplayer.ui

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
import androidx.navigation.NavController
import com.fuentes.battleships.modules.auth.ui.AuthViewModel
import com.fuentes.battleships.modules.game.singleplayer.data.Cell
import com.fuentes.battleships.modules.game.singleplayer.data.GamePhase
import com.fuentes.battleships.modules.game.singleplayer.data.BoardView
import com.fuentes.battleships.modules.game.singleplayer.data.GameLogic
import com.fuentes.battleships.modules.game.singleplayer.data.GameState
import kotlinx.coroutines.delay

@Composable
fun OpponentScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    navController: NavController
) {
    val gameLogic = GameLogic()
    var gameBoard by remember { mutableStateOf(gameLogic.createInitialBoard()) }
    var gameState by remember { mutableStateOf(GameState()) }
    var showHitMissDialog by remember { mutableStateOf(false) }
    var hitMissMessage by remember { mutableStateOf("") }
    var showTurnNotification by remember { mutableStateOf(false) }
    var turnMessage by remember { mutableStateOf("") }
    var timer by remember { mutableStateOf(15) } // 15-second timer
    var isTimerRunning by remember { mutableStateOf(false) } // Timer state

    // Timer logic
    LaunchedEffect(isTimerRunning, timer) {
        if (isTimerRunning && timer > 0) {
            delay(1000L) // Wait for 1 second
            timer-- // Decrement timer
        } else if (timer == 0) {
            // Timer ran out, switch turns
            isTimerRunning = false
            gameState = gameState.copy(isPlayer1Turn = !gameState.isPlayer1Turn)
            timer = 15 // Reset timer
            showTurnNotification = true
            turnMessage = "Time's up! Player ${if (!gameState.isPlayer1Turn) 1 else 2}'s turn"
        }
    }

    // Modified LaunchedEffect to automatically set attack mode when turn starts
    LaunchedEffect(gameState.isPlayer1Turn, gameState.phase) {
        if (gameState.phase == 0) {
            val player1ShipsCount = gameState.player1Ships.size
            val player2ShipsCount = gameState.player2Ships.size

            if (player1ShipsCount == 2 && !showTurnNotification) {
                // Show simple turn notification for Player 2
                showTurnNotification = true
                turnMessage = "Player 2's turn to place ships"

                // After showing notification, initialize Player 2's board as empty
                gameBoard = gameLogic.createInitialBoard()
            }
        } else if (gameState.phase == 1) {
            // Automatically set to attack mode when a player's turn begins
            if (!showTurnNotification && !showHitMissDialog) {
                gameState = gameState.copy(boardView = 1)
                gameBoard = gameLogic.createBoardForAttacking(gameState)
                isTimerRunning = true // Start the timer automatically
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // Game header - removed username references
            Text(
                text = when (gameState.phase) {
                    0 -> {
                        val playerShipsCount = if (gameState.isPlayer1Turn)
                            gameState.player1Ships.size
                        else
                            gameState.player2Ships.size
                        "Player ${if (gameState.isPlayer1Turn) 1 else 2} Place Your Ship (${playerShipsCount}/2)"
                    }
                    1 -> {
                        "Player ${if (gameState.isPlayer1Turn) 1 else 2}'s Attack Turn - Time Left: $timer seconds"
                    }
                    else -> "Game Over! Player ${if (gameState.isPlayer1Turn) 1 else 2} Wins!"
                },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Ship placement controls
            if (gameState.phase == 0) {
                Button(
                    onClick = {
                        gameState = gameState.copy(isHorizontal = !gameState.isHorizontal)
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(if (gameState.isHorizontal) "Horizontal Placement" else "Vertical Placement")
                }
            }

            // Removed "Attack Enemy" button since we auto-transition to attack mode

            Spacer(modifier = Modifier.height(8.dp))

            // Game Board legend
            Card(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Legend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(color = Color(0xFF81D4FA), text = "Water")
                        LegendItem(color = Color(0xFF0C4870), text = "Ship")
                        LegendItem(color = Color(0xFFD32F2F), text = "Hit")
                        LegendItem(color = Color(0xFF9E9E9E), text = "Miss")
                    }
                }
            }

            // Board Title
            if (gameState.phase == 1) {
                Text(
                    text = "Enemy Waters (Click to Attack!)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Game Board
            Box {
                BattleshipGrid(
                    board = gameBoard,
                    enabled = when (gameState.phase) {
                        0 -> true
                        1 -> true  // Always enabled during battle phase since we're always in attack mode
                        else -> false
                    },
                    onCellClick = { x, y ->
                        when (gameState.phase) {
                            0 -> gameLogic.handlePlacement(x, y, gameState, gameBoard) { newState, newBoard ->
                                gameState = newState
                                gameBoard = newBoard
                            }
                            1 -> {
                                gameLogic.handleAttack(x, y, gameState, gameBoard) { newState, newBoard, isHit ->

                                    // Display hit/miss notification
                                    hitMissMessage = if (isHit) "Hit! You found an enemy ship!" else "Miss! No ship at this location."
                                    showHitMissDialog = true

                                    gameState = newState
                                    gameBoard = newBoard

                                    // Check for game over
                                    if (newState.phase == 2) {
                                        hitMissMessage = "Game Over! Player ${if (gameState.isPlayer1Turn) 1 else 2} Wins!"
                                    }

                                    // Reset timer after attack
                                    isTimerRunning = false
                                    timer = 15
                                }
                            }
                            2 -> { /* Do nothing */ }
                        }
                    }
                )

                BlockingOverlay(
                    isBlocking = gameState.phase == 1 && gameState.isPlayer1Turn,
                    message = "Waiting for Player 1's turn..."
                )
            }

            if (gameState.phase == 2) {
                Button(
                    onClick = {
                        gameState = GameState()
                        gameBoard = gameLogic.createInitialBoard()
                        timer = 15 // Reset timer
                        isTimerRunning = false
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Play Again")
                }
            }
        }
    }

    // Simplified turn notification
    if (showTurnNotification) {
        SimpleTurnNotification(
            message = turnMessage,
            onDismiss = {
                showTurnNotification = false

                // When a notification is dismissed, automatically set to attack mode for battle phase
                if (gameState.phase == 2) {
                    gameState = gameState.copy(boardView = 1)
                    gameBoard = gameLogic.createBoardForAttacking(gameState)
                    isTimerRunning = true // Start the timer
                }
            }
        )
    }

    // Hit/miss dialog
    if (showHitMissDialog) {
        HitMissDialog(
            message = hitMissMessage,
            onDismiss = {
                showHitMissDialog = false

                // If game is over, don't switch players
                if (gameState.phase != 2) {
                    // Show turn notification after hit/miss message
                    showTurnNotification = true
                    turnMessage = "Player ${if (!gameState.isPlayer1Turn) 1 else 2}'s turn"
                }
            }
        )
    }
}


