package com.fuentes.battleships.modules.game.multiplayer.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.fuentes.battleships.modules.game.multiplayer.data.GameSession
import com.fuentes.battleships.modules.game.multiplayer.data.GameViewModel
import com.fuentes.battleships.modules.game.singleplayer.data.Cell
import kotlinx.coroutines.delay

@Composable
fun MultiplayerScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    navController: NavController,
    gameViewModel: GameViewModel,
    sessionId: String
) {
    val gameSession by gameViewModel.gameSession.collectAsState()
    val currentUserEmail by authViewModel.uiState.collectAsState()
    val currentUserId = currentUserEmail.userId

    var showHitMissDialog by remember { mutableStateOf(false) }
    var hitMissMessage by remember { mutableStateOf("") }
    var timer by remember { mutableStateOf(15) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Timer logic
    LaunchedEffect(isTimerRunning, timer, gameSession.isPlayer1Turn) {
        if (isTimerRunning && timer > 0) {
            delay(1000L)
            timer--
        } else if (timer == 0) {
            // Time's up, handle timer expiration for both players
            isTimerRunning = false
            gameViewModel.handleTimerExpired(gameSession)
            timer = 15 // Reset the local timer state
        }
    }

    LaunchedEffect(sessionId) {
        gameViewModel.initializeGame(sessionId)
    }

    LaunchedEffect(gameSession.isPlayer1Turn, gameSession.phase) {
        if (gameSession.phase == 1) {
            isTimerRunning = true
            timer = gameSession.timer // Sync the local timer with the session timer
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp, 48.dp, 16.dp, 16.dp)
        ) {
            Text(
                text = when (gameSession.phase) {
                    0 -> "Player ${if (gameSession.isPlayer1Turn) 1 else 2} Place Your Ship (${if (gameSession.isPlayer1Turn) gameSession.player1Ships.size else gameSession.player2Ships.size}/2)"
                    1 -> "Player ${if (gameSession.isPlayer1Turn) 1 else 2}'s Attack Turn - Time Left: $timer seconds"
                    else -> "Game Over! Player ${if (gameSession.isPlayer1Turn) 1 else 2} Wins!"
                },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (gameSession.phase == 0) {
                Button(
                    onClick = {
                        gameViewModel.updateGameSession(gameSession.copy(isHorizontal = !gameSession.isHorizontal))
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(if (gameSession.isHorizontal) "Horizontal Placement" else "Vertical Placement")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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

            if (gameSession.phase == 1) {
                Text(
                    text = "Enemy Waters (Click to Attack!)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            BattleshipGrid(
                board = createBoardForMultiplayer(
                    gameSession,
                    currentUserId == gameSession.player1Id
                ),
                enabled = gameSession.phase != 2 && ((gameSession.isPlayer1Turn && currentUserId == gameSession.player1Id) || (!gameSession.isPlayer1Turn && currentUserId == gameSession.player2Id)),
                onCellClick = { index ->
                    when (gameSession.phase) {
                        0 -> gameViewModel.handlePlacement(index, gameSession, gameViewModel)
                        1 -> gameViewModel.handleAttack(index, gameSession, gameViewModel) { isHit ->
                            hitMissMessage =
                                if (isHit) "Hit! You found an enemy ship!" else "Miss! No ship at this location."
                            showHitMissDialog = true
                        }
                    }
                }
            )

            Text(
                text = hitMissMessage,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )

            if (gameSession.phase == 2) {
                Button(
                    onClick = {
                        gameViewModel.resetGameSession(sessionId)
                        navController.popBackStack()
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Play Again")
                }
            }
        }
    }

    // Use the shared turn notification state from the GameSession
    if (gameSession.showTurnNotification) {
        SimpleTurnNotification(
            message = gameSession.turnMessage,
            onDismiss = {
                gameViewModel.dismissTurnNotification(gameSession)
                if (gameSession.phase == 1) {
                    isTimerRunning = true
                }
            }
        )
    }
}

@Composable
fun createBoardForMultiplayer(gameSession: GameSession, isPlayer1: Boolean): List<Cell> {
    val attackedShips = if (isPlayer1) gameSession.player2Ships else gameSession.player1Ships
    val attackingHits = if (isPlayer1) gameSession.player1Hits else gameSession.player2Hits
    val attackingMisses = if (isPlayer1) gameSession.player1Misses else gameSession.player2Misses

    return List(100) { index ->
        val isHit = index in attackingHits
        val isMiss = index in attackingMisses
        val isShip = isHit && attackedShips.any { ship -> index in ship }

        Cell(
            index = index,
            isShip = isShip,
            isHit = isHit,
            isMiss = isMiss
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

@Composable
fun SimpleTurnNotification(message: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@Composable
fun HitMissDialog(message: String, onDismiss: () -> Unit) {
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
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
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
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Continue", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun BattleshipGrid(board: List<Cell>, enabled: Boolean, onCellClick: (Int) -> Unit) {
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
        items(11 * 11) { index ->
            val row = index / 11
            val col = index % 11

            if (row == 0 && col == 0) {
                Box(modifier = Modifier.size(35.dp))
            } else if (row == 0) {
                Box(modifier = Modifier.size(35.dp))
            } else if (col == 0) {
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
                val cellIndex = (row - 1) * 10 + (col - 1)
                if (cellIndex < board.size) {
                    val cell = board[cellIndex]
                    BattleshipCell(
                        cell = cell,
                        enabled = enabled,
                        onClick = { onCellClick(cell.index) }
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
