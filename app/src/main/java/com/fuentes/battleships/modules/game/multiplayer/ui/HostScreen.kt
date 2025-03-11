package com.fuentes.battleships.modules.game.multiplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
fun HostScreen(
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
                    isBlocking = gameState.phase == 1 && !gameState.isPlayer1Turn,
                    message = "Waiting for Player 2's turn..."
                )
            }

            // Game Board


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


@Composable
fun LegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(color, shape = MaterialTheme.shapes.small)
                .border(1.dp, Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
        )
        Text(
            " = $text",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

// Simplified turn notification to replace the previous dialog
@Composable
fun SimpleTurnNotification(
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(0.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@Composable
fun HitMissDialog(
    message: String,
    onDismiss: () -> Unit
) {
    val isHit = message.contains("Hit")
    val isMiss = message.contains("Miss")
    val isGameOver = message.contains("Game Over")

    val backgroundColor = when {
        isHit -> Color(0xFFEF9A9A)
        isMiss -> Color(0xFFBBDEFB)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val buttonColor = when {
        isHit -> Color(0xFFC62828)
        isMiss -> Color(0xFF1976D2)
        else -> MaterialTheme.colorScheme.primary
    }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isHit) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Hit",
                        tint = Color(0xFFC62828),
                        modifier = Modifier
                            .size(48.dp)
                            .padding(bottom = 16.dp)
                    )
                } else if (isMiss) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF1976D2), CircleShape)
                            .padding(bottom = 16.dp)
                    )
                }

                Text(
                    text = message,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(0.7f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "Continue",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun BattleshipCell(
    cell: Cell,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val cellColor = when {
        cell.isHit && cell.isShip -> Color(0xFFD32F2F) // Brighter red for hits
        cell.isMiss -> Color(0xFF9E9E9E) // Lighter gray for misses
        cell.isShip -> Color(0xFF1E88E5) // Brighter blue for ships
        else -> Color(0xFF81D4FA) // Light blue water
    }

    Button(
        onClick = onClick,
        modifier = Modifier
            .size(35.dp)
            .aspectRatio(1f)
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.small
            ),
        enabled = enabled && !cell.isHit && !cell.isMiss,
        colors = ButtonDefaults.buttonColors(
            containerColor = cellColor,
            disabledContainerColor = cellColor
        ),
        shape = MaterialTheme.shapes.small,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        )
    ) {
        if (cell.isHit && cell.isShip) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Hit",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        } else if (cell.isMiss) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}


@Composable
fun BattleshipGrid(
    board: List<Cell>,
    enabled: Boolean,
    onCellClick: (Int, Int) -> Unit
) {
    // Column headers
    Row(
        modifier = Modifier
            .padding(start = 40.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0..9) {
            Text(
                text = "${('A' + i)}",
                modifier = Modifier.size(35.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(11),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        // Generate 11x11 grid (10x10 game board + row/column headers)
        items(11 * 11) { index ->
            val row = index / 11
            val col = index % 11

            if (row == 0 && col == 0) {
                // Top-left corner is empty
                Box(modifier = Modifier.size(35.dp))
            } else if (row == 0) {
                // Column headers - already handled above
                Box(modifier = Modifier.size(35.dp))
            } else if (col == 0) {
                // Row numbers
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(35.dp)
                ) {
                    Text(
                        text = "${row}",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // Game cells
                val cellIndex = (row - 1) * 10 + (col - 1)
                if (cellIndex < board.size) {
                    val cell = board[cellIndex]
                    BattleshipCell(
                        cell = cell,
                        enabled = enabled,
                        onClick = { onCellClick(cell.x, cell.y) }
                    )
                }
            }
        }
    }
}

@Composable
fun BlockingOverlay(
    isBlocking: Boolean,
    message: String
) {
    if (isBlocking) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { /* Do nothing to block interactions */ },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
