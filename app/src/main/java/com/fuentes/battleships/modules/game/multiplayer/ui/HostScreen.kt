/*
package com.fuentes.battleships.modules.game.multiplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fuentes.battleships.modules.game.GameLogic
import com.fuentes.battleships.modules.game.multiplayer.data.GameViewModel
import com.fuentes.battleships.modules.game.singleplayer.data.Cell
import com.fuentes.battleships.modules.game.multiplayer.data.GameSession
import kotlinx.coroutines.delay

@Composable
fun HostScreen(
    navController: NavController,
    gameViewModel: GameViewModel,
    sessionId: String
) {
    // Observe game session updates in real-time
    val gameSession by gameViewModel.gameSession.collectAsState()
    val gameLogic = remember { GameLogic() }

    // Initialize board based on current player's view
    var gameBoard by remember { mutableStateOf(emptyList<Cell>()) }
    var showHitMissDialog by remember { mutableStateOf(false) }
    var hitMissMessage by remember { mutableStateOf("") }
    var timer by remember { mutableStateOf(15) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Get current user ID from the game session
    val isPlayer1 = remember(gameSession) {
        gameViewModel.isCurrentUserPlayer1(gameSession)
    }

    // Initialize board based on the game session data
    LaunchedEffect(gameSession) {
        gameBoard = when {
            // Phase 0: Placement Phase - Show own board
            gameSession.phase == 0 -> {
                val playerShips = if (isPlayer1) gameSession.player1Ships else gameSession.player2Ships
                val opponentHits = if (isPlayer1) gameSession.player2Hits else gameSession.player1Hits
                val opponentMisses = if (isPlayer1) gameSession.player2Misses else gameSession.player1Misses

                gameLogic.createBoardWithShips(playerShips, opponentHits, opponentMisses)
            }
            // Phase 1: Battle Phase - Show appropriate board based on turn
            gameSession.phase == 1 -> {
                if ((isPlayer1 && gameSession.isPlayer1Turn) || (!isPlayer1 && !gameSession.isPlayer1Turn)) {
                    // My turn - show opponent's board for attacking
                    gameLogic.createBoardForAttackingMultiplayer(gameSession)
                } else {
                    // Opponent's turn - show my board
                    val playerShips = if (isPlayer1) gameSession.player1Ships else gameSession.player2Ships
                    val opponentHits = if (isPlayer1) gameSession.player2Hits else gameSession.player1Hits
                    val opponentMisses = if (isPlayer1) gameSession.player2Misses else gameSession.player1Misses

                    gameLogic.createBoardWithShips(playerShips, opponentHits, opponentMisses)
                }
            }
            // Game over - show final state
            else -> gameLogic.createBoardForAttackingMultiplayer(gameSession)
        }
    }

    // Timer logic
    LaunchedEffect(gameSession.isPlayer1Turn, gameSession.phase) {
        // Start timer when it's player's turn in battle phase
        isTimerRunning = gameSession.phase == 1 &&
                ((isPlayer1 && gameSession.isPlayer1Turn) || (!isPlayer1 && !gameSession.isPlayer1Turn))

        // Reset timer value
        if (isTimerRunning) {
            timer = gameSession.timer
        }
    }

    // Countdown timer
    LaunchedEffect(isTimerRunning, timer) {
        if (isTimerRunning && timer > 0) {
            delay(1000L)
            timer--

            // Update timer in database every 5 seconds
            if (timer % 5 == 0) {
                gameViewModel.updateTimer(gameSession.sessionId ?: "", timer)
            }

            // Auto-switch turn if time runs out
            if (timer == 0) {
                isTimerRunning = false
                gameViewModel.switchTurn(gameSession)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Game status header
        Text(
            text = when (gameSession.phase) {
                0 -> "Ship Placement Phase"
                1 -> if ((isPlayer1 && gameSession.isPlayer1Turn) || (!isPlayer1 && !gameSession.isPlayer1Turn))
                    "Your Turn - Time Left: $timer seconds"
                else
                    "Opponent's Turn - Please Wait"
                else -> if ((isPlayer1 && gameSession.player1Hits.size == 6) ||
                    (!isPlayer1 && gameSession.player2Hits.size == 6))
                    "Game Over - You Won!" else "Game Over - You Lost"
            },
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Board view toggle button for battle phase
        if (gameSession.phase == 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        // Toggle view between your board and opponent's board
                        if ((isPlayer1 && gameSession.isPlayer1Turn) || (!isPlayer1 && !gameSession.isPlayer1Turn)) {
                            // Show opponent's board (for attacking)
                            gameBoard = gameLogic.createBoardForAttackingMultiplayer(gameSession)
                        } else {
                            // Show your own board
                            val playerShips = if (isPlayer1) gameSession.player1Ships else gameSession.player2Ships
                            val opponentHits = if (isPlayer1) gameSession.player2Hits else gameSession.player1Hits
                            val opponentMisses = if (isPlayer1) gameSession.player2Misses else gameSession.player1Misses

                            gameBoard = gameLogic.createBoardWithShips(playerShips, opponentHits, opponentMisses)
                        }
                    }
                ) {
                    Text("Toggle View")
                }

                // Ship rotation button for placement phase
                if (gameSession.phase == 0) {
                    Button(
                        onClick = {
                            gameViewModel.toggleShipOrientation(gameSession.sessionId ?: "")
                        }
                    ) {
                        Text(if (gameSession.isHorizontal) "Horizontal" else "Vertical")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Game board grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(10),
            modifier = Modifier.padding(8.dp)
        ) {
            items(100) { index ->
                val x = index % 10
                val y = index / 10
                val cell = gameBoard.getOrNull(index) ?: Cell(x, y)

                BattleshipCell(
                    cell = cell,
                    enabled = when (gameSession.phase) {
                        // Placement phase - only enabled for current player's turn
                        0 -> (isPlayer1 && gameSession.isPlayer1Turn) || (!isPlayer1 && !gameSession.isPlayer1Turn)
                            // Battle phase - only enabled when it's player's turn and cell hasn't been attacked yet
                        1 -> ((isPlayer1 && gameSession.isPlayer1Turn) || (!isPlayer1 && !gameSession.isPlayer1Turn)) &&
                                    !cell.isHit && !cell.isMiss
                            // Game over - disabled
                        else -> false
                    },
                    onClick = {
                        when (gameSession.phase) {
                            0 -> {
                                // Handle ship placement
                                gameViewModel.placeShip(gameSession.sessionId ?: "", x, y)
                            }
                            1 -> {
                                // Handle attack
                                gameViewModel.attackCell(gameSession.sessionId ?: "", x, y) { isHit ->
                                    hitMissMessage = if (isHit) "Hit!" else "Miss!"
                                    showHitMissDialog = true
                                }
                            }
                        }
                    }
                )
            }
        }

        // Game over button
        if (gameSession.phase == 2) {
            Button(
                onClick = { navController.navigate("home") },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Return to Home")
            }
        }

        // Hit/Miss dialog
        if (showHitMissDialog) {
            AlertDialog(
                onDismissRequest = { showHitMissDialog = false },
                confirmButton = {
                    Button(onClick = { showHitMissDialog = false }) {
                        Text("OK")
                    }
                },
                title = { Text("Attack Result") },
                text = { Text(hitMissMessage) }
            )
        }
    }
}

// Battleship Cell UI Component
@Composable
fun BattleshipCell(cell: Cell, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(35.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                cell.isHit && cell.isShip -> MaterialTheme.colorScheme.error
                cell.isMiss -> MaterialTheme.colorScheme.secondary
                cell.isShip -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Text(if (cell.isHit) "X" else if (cell.isMiss) "O" else "")
    }
}*/
