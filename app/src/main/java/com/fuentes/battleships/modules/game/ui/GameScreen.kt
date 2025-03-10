package com.fuentes.battleships.modules.game.ui

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
import com.fuentes.battleships.modules.game.data.models.Cell
import com.fuentes.battleships.modules.game.data.models.GamePhase
import com.fuentes.battleships.modules.game.data.models.BoardView
import kotlinx.coroutines.delay
@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    navController: NavController
) {
    var gameBoard by remember { mutableStateOf(createInitialBoard()) }
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
        if (gameState.phase == GamePhase.PLACEMENT && gameState.player1.ships.size == 2 && !showTurnNotification) {
            // Show simple turn notification for Player 2
            showTurnNotification = true
            turnMessage = "Player 2's turn to place ships"

            // After showing notification, initialize Player 2's board as empty
            gameBoard = createInitialBoard()
        } else if (gameState.phase == GamePhase.BATTLE) {
            // Automatically set to attack mode when a player's turn begins
            if (!showTurnNotification && !showHitMissDialog) {
                gameState = gameState.copy(boardView = BoardView.OPPONENT_BOARD)
                gameBoard = createBoardForAttacking(gameState)
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
                    GamePhase.PLACEMENT -> {
                        "Player ${if (gameState.isPlayer1Turn) 1 else 2} Place Your Ship (${if (gameState.isPlayer1Turn) gameState.player1.ships.size else gameState.player2.ships.size}/2)"
                    }
                    GamePhase.BATTLE -> {
                        "Player ${if (gameState.isPlayer1Turn) 1 else 2}'s Attack Turn - Time Left: $timer seconds"
                    }
                    GamePhase.GAME_OVER -> "Game Over! Player ${if (gameState.isPlayer1Turn) 1 else 2} Wins!"
                },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Ship placement controls
            if (gameState.phase == GamePhase.PLACEMENT) {
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
            if (gameState.phase == GamePhase.BATTLE) {
                Text(
                    text = "Enemy Waters (Click to Attack!)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Game Board
            BattleshipGrid(
                board = gameBoard,
                enabled = when (gameState.phase) {
                    GamePhase.PLACEMENT -> true
                    GamePhase.BATTLE -> true  // Always enabled during battle phase since we're always in attack mode
                    GamePhase.GAME_OVER -> false
                },
                onCellClick = { x, y ->
                    when (gameState.phase) {
                        GamePhase.PLACEMENT -> handlePlacement(x, y, gameState, gameBoard) { newState, newBoard ->
                            gameState = newState
                            gameBoard = newBoard
                        }
                        GamePhase.BATTLE -> {
                            handleAttack(x, y, gameState, gameBoard) { newState, newBoard, isHit ->
                                val attackedPlayer = if (gameState.isPlayer1Turn) gameState.player2 else gameState.player1
                                val attackingPlayer = if (gameState.isPlayer1Turn) gameState.player1 else gameState.player2

                                // Display hit/miss notification
                                hitMissMessage = if (isHit) "Hit! You found an enemy ship!" else "Miss! No ship at this location."
                                showHitMissDialog = true

                                gameState = newState
                                gameBoard = newBoard

                                // Check for game over
                                if (newState.phase == GamePhase.GAME_OVER) {
                                    hitMissMessage = "Game Over! Player ${if (gameState.isPlayer1Turn) 1 else 2} Wins!"
                                }

                                // Reset timer after attack
                                isTimerRunning = false
                                timer = 15
                            }
                        }
                        GamePhase.GAME_OVER -> { /* Do nothing */ }
                    }
                }
            )

            if (gameState.phase == GamePhase.GAME_OVER) {
                Button(
                    onClick = {
                        gameState = GameState()
                        gameBoard = createInitialBoard()
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
                if (gameState.phase == GamePhase.BATTLE) {
                    gameState = gameState.copy(boardView = BoardView.OPPONENT_BOARD)
                    gameBoard = createBoardForAttacking(gameState)
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
                if (gameState.phase != GamePhase.GAME_OVER) {
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

private fun handlePlacement(
    x: Int,
    y: Int,
    gameState: GameState,
    board: List<Cell>,
    onUpdate: (GameState, List<Cell>) -> Unit
) {
    val currentPlayer = if (gameState.isPlayer1Turn) gameState.player1 else gameState.player2

    if (currentPlayer.ships.size < 2) {
        val shipPositions = calculateShipPositions(x, y, gameState.isHorizontal)

        // Check if ship would be out of bounds
        if (shipPositions.all { (posX, posY) -> posX in 0..9 && posY in 0..9 }) {
            // Check if ship overlaps with existing ships
            val existingShipPositions = currentPlayer.ships.flatten()
            if (shipPositions.none { it in existingShipPositions }) {
                val newShips = currentPlayer.ships + listOf(shipPositions)
                val newPlayer = currentPlayer.copy(ships = newShips)

                // Update board to show ship
                val newBoard = board.map { cell ->
                    if (shipPositions.any { (shipX, shipY) -> shipX == cell.x && shipY == cell.y }) {
                        cell.copy(isShip = true)
                    } else cell
                }

                var newState = if (gameState.isPlayer1Turn) {
                    gameState.copy(player1 = newPlayer)
                } else {
                    gameState.copy(player2 = newPlayer)
                }

                // Switch turns or phases
                if (newPlayer.ships.size == 2) {
                    if (!gameState.isPlayer1Turn) {
                        // Both players have placed their ships
                        newState = newState.copy(
                            phase = GamePhase.BATTLE,
                            isPlayer1Turn = true,
                            boardView = BoardView.OWN_BOARD
                        )
                    } else {
                        // Switch to player 2's placement
                        newState = newState.copy(isPlayer1Turn = false)
                    }
                }
                onUpdate(newState, newBoard)
            }
        }
    }
}

private fun handleAttack(
    x: Int,
    y: Int,
    gameState: GameState,
    board: List<Cell>,
    onUpdate: (GameState, List<Cell>, Boolean) -> Unit
) {
    val attackedPlayer = if (gameState.isPlayer1Turn) gameState.player2 else gameState.player1
    val attackingPlayer = if (gameState.isPlayer1Turn) gameState.player1 else gameState.player2
    val coordinate = Pair(x, y)

    // Check if cell was already attacked
    if (coordinate !in attackingPlayer.hits && coordinate !in attackingPlayer.misses) {
        // Check if hit by looking through all ships
        val isHit = attackedPlayer.ships.any { ship -> coordinate in ship }

        val newAttackingPlayer = attackingPlayer.copy(
            hits = if (isHit) attackingPlayer.hits + coordinate else attackingPlayer.hits,
            misses = if (!isHit) attackingPlayer.misses + coordinate else attackingPlayer.misses
        )

        // Update board and state
        val newBoard = board.map { cell ->
            if (cell.x == x && cell.y == y) {
                cell.copy(
                    isHit = isHit,
                    isMiss = !isHit,
                    // Show ship if it was hit
                    isShip = isHit && attackedPlayer.ships.any { ship ->
                        ship.any { (shipX, shipY) -> shipX == cell.x && shipY == cell.y }
                    }
                )
            } else cell
        }

        var newState = if (gameState.isPlayer1Turn) {
            gameState.copy(player1 = newAttackingPlayer)
        } else {
            gameState.copy(player2 = newAttackingPlayer)
        }

        // Count hits to check for win condition
        // Each player has 2 ships × 3 cells = 6 cells total
        val totalShipCells = 6

        // Check for game over
        newState = if (newAttackingPlayer.hits.size == totalShipCells) {
            newState.copy(phase = GamePhase.GAME_OVER)
        } else {
            // Switch turns
            newState.copy(isPlayer1Turn = !newState.isPlayer1Turn, boardView = BoardView.OWN_BOARD)
        }

        onUpdate(newState, newBoard, isHit)
    }
}

private fun createBoardWithShips(
    ships: List<List<Pair<Int, Int>>>,
    opponentHits: List<Pair<Int, Int>>,
    opponentMisses: List<Pair<Int, Int>>
): List<Cell> {
    return createInitialBoard().map { cell ->
        val coordinate = Pair(cell.x, cell.y)
        val isShip = ships.any { ship ->
            ship.any { (shipX, shipY) -> shipX == cell.x && shipY == cell.y }
        }
        val isHit = isShip && coordinate in opponentHits
        val isMiss = coordinate in opponentMisses

        cell.copy(
            isShip = isShip,
            isHit = isHit,
            isMiss = isMiss
        )
    }
}

private fun createBoardForAttacking(gameState: GameState): List<Cell> {
    val attackedPlayer = if (gameState.isPlayer1Turn) gameState.player2 else gameState.player1
    val attackingPlayer = if (gameState.isPlayer1Turn) gameState.player1 else gameState.player2

    return createInitialBoard().map { cell ->
        val coordinate = Pair(cell.x, cell.y)
        val isHit = coordinate in attackingPlayer.hits
        val isMiss = coordinate in attackingPlayer.misses

        // Only mark as ship if it was hit
        val isShip = isHit && attackedPlayer.ships.any { ship -> coordinate in ship }

        cell.copy(
            // Never show enemy ships unless they were hit
            isShip = isShip,
            isHit = isHit,
            isMiss = isMiss
        )
    }
}

fun calculateShipPositions(startX: Int, startY: Int, isHorizontal: Boolean): List<Pair<Int, Int>> {
    return if (isHorizontal) {
        listOf(
            Pair(startX, startY),
            Pair(startX + 1, startY),
            Pair(startX + 2, startY)
        )
    } else {
        listOf(
            Pair(startX, startY),
            Pair(startX, startY + 1),
            Pair(startX, startY + 2)
        )
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

fun createInitialBoard(): List<Cell> {
    val board = mutableListOf<Cell>()
    for (y in 0..9) {
        for (x in 0..9) {
            board.add(Cell(x, y))
        }
    }
    return board
}