package com.fuentes.battleships.models.game.ui

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.fuentes.battleships.models.auth.ui.AuthViewModel
import com.fuentes.battleships.ui.theme.BattleshipsTheme
import com.fuentes.battleships.models.game.data.Cell
import com.fuentes.battleships.models.game.data.Player
import com.fuentes.battleships.models.game.data.GameState
import com.fuentes.battleships.models.game.data.GamePhase
import com.fuentes.battleships.models.game.data.BoardView


@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    var gameBoard by remember { mutableStateOf(createInitialBoard()) }
    var gameState by remember { mutableStateOf(GameState()) }
    var showSwitchPlayerDialog by remember { mutableStateOf(false) }
    var currentPlayerText by remember { mutableStateOf("") }
    var showHitMissDialog by remember { mutableStateOf(false) }
    var hitMissMessage by remember { mutableStateOf("") }

    // Username state management
    var showUsernameDialog by remember { mutableStateOf(true) }
    var currentPlayerNumber by remember { mutableStateOf(1) }

    // Ask for player 1 username
    if (showUsernameDialog && currentPlayerNumber == 1) {
        UsernameDialog(
            playerNumber = 1,
            onSubmit = { username ->
                // Set player 1 username
                gameState = gameState.copy(
                    player1 = gameState.player1.copy(username = username)
                )
                currentPlayerNumber = 2
            }
        )
    }

    // Ask for player 2 username
    if (showUsernameDialog && currentPlayerNumber == 2) {
        UsernameDialog(
            playerNumber = 2,
            onSubmit = { username ->
                // Set player 2 username
                gameState = gameState.copy(
                    player2 = gameState.player2.copy(username = username)
                )
                showUsernameDialog = false
            }
        )
    }

    // 6. Update the LaunchedEffect to use usernames for the player switching message
    LaunchedEffect(gameState.isPlayer1Turn, gameState.phase) {
        if (gameState.phase == GamePhase.PLACEMENT && gameState.player1.ships.size == 2 && !showSwitchPlayerDialog) {
            showSwitchPlayerDialog = true
            currentPlayerText = "${gameState.player2.username}'s turn to place ships"
        } else if (gameState.phase == GamePhase.BATTLE && gameState.boardView == BoardView.OWN_BOARD) {
            // Update the board view to match the current player's ships
            gameBoard = createBoardWithShips(
                if (gameState.isPlayer1Turn) gameState.player1.ships else gameState.player2.ships,
                if (gameState.isPlayer1Turn) gameState.player2.hits else gameState.player1.hits,
                if (gameState.isPlayer1Turn) gameState.player2.misses else gameState.player1.misses
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // Game header
            Text(
                text = when (gameState.phase) {
                    GamePhase.PLACEMENT -> {
                        val currentPlayer = if (gameState.isPlayer1Turn) gameState.player1 else gameState.player2
                        "${currentPlayer.username} Place Your Ship (${currentPlayer.ships.size}/2)"
                    }
                    GamePhase.BATTLE -> {
                        if (gameState.boardView == BoardView.OWN_BOARD) {
                            "${if (gameState.isPlayer1Turn) gameState.player1.username else gameState.player2.username}'s Fleet"
                        } else {
                            "${if (gameState.isPlayer1Turn) gameState.player1.username else gameState.player2.username}'s Attack Turn"
                        }
                    }
                    GamePhase.GAME_OVER -> "Game Over! ${if (gameState.isPlayer1Turn) gameState.player1.username else gameState.player2.username} Wins!"
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

            // Battle phase controls - switch between viewing your ships and attacking
            if (gameState.phase == GamePhase.BATTLE && gameState.phase != GamePhase.GAME_OVER) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            gameState = gameState.copy(boardView = BoardView.OWN_BOARD)
                            gameBoard = createBoardWithShips(
                                if (gameState.isPlayer1Turn) gameState.player1.ships else gameState.player2.ships,
                                if (gameState.isPlayer1Turn) gameState.player2.hits else gameState.player1.hits,
                                if (gameState.isPlayer1Turn) gameState.player2.misses else gameState.player1.misses
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (gameState.boardView == BoardView.OWN_BOARD)
                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("View Your Fleet")
                    }

                    Button(
                        onClick = {
                            gameState = gameState.copy(boardView = BoardView.OPPONENT_BOARD)
                            gameBoard = createBoardForAttacking(gameState)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (gameState.boardView == BoardView.OPPONENT_BOARD)
                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Attack Enemy")
                    }
                }
            }

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
                    text = when (gameState.boardView) {
                        BoardView.OWN_BOARD -> "Your Fleet (X = where enemy attacked)"
                        BoardView.OPPONENT_BOARD -> "Enemy Waters (Click to Attack!)"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Game Board
            BattleshipGrid(
                board = gameBoard,
                enabled = when (gameState.phase) {
                    GamePhase.PLACEMENT -> true
                    GamePhase.BATTLE -> gameState.boardView == BoardView.OPPONENT_BOARD
                    GamePhase.GAME_OVER -> false
                },
                onCellClick = { x, y ->
                    when (gameState.phase) {
                        GamePhase.PLACEMENT -> handlePlacement(x, y, gameState, gameBoard) { newState, newBoard ->
                            gameState = newState
                            gameBoard = newBoard
                        }
                        GamePhase.BATTLE -> {
                            if (gameState.boardView == BoardView.OPPONENT_BOARD) {
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
                                        hitMissMessage = "Game Over! ${if (gameState.isPlayer1Turn) "Player 1" else "Player 2"} Wins!"
                                    }
                                }
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
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Play Again")
                }
            }
        }
    }

    if (showSwitchPlayerDialog) {
        PlayerSwitchDialog(
            title = "Switch Players",
            message = currentPlayerText,
            onConfirm = {
                showSwitchPlayerDialog = false
                if (gameState.phase == GamePhase.PLACEMENT && gameState.player1.ships.size == 2) {
                    // Initialize Player 2's board as empty
                    gameBoard = createInitialBoard()
                } else if (gameState.phase == GamePhase.BATTLE) {
                    // Start with viewing your own ships
                    gameState = gameState.copy(boardView = BoardView.OWN_BOARD)
                    gameBoard = createBoardWithShips(
                        if (gameState.isPlayer1Turn) gameState.player1.ships else gameState.player2.ships,
                        if (gameState.isPlayer1Turn) gameState.player2.hits else gameState.player1.hits,
                        if (gameState.isPlayer1Turn) gameState.player2.misses else gameState.player1.misses
                    )
                }
            }
        )
    }

    // 7. Update the hit/miss dialog section to use usernames
    if (showHitMissDialog) {
        HitMissDialog(
            message = hitMissMessage,
            onDismiss = {
                showHitMissDialog = false

                // If game is over, don't switch players
                if (gameState.phase != GamePhase.GAME_OVER) {
                    // Show switch player dialog after hit/miss message
                    showSwitchPlayerDialog = true
                    currentPlayerText = "${if (!gameState.isPlayer1Turn) gameState.player1.username else gameState.player2.username}'s turn"
                }
            }
        )
    }
}

@Composable
fun UsernameDialog(
    playerNumber: Int,
    onSubmit: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = { /* Do nothing, force entry */ },
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                Text(
                    text = "Welcome to Battleships!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Player $playerNumber, enter your name:",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Username text field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                Button(
                    onClick = {
                        // Use default name if empty
                        if (username.isBlank()) {
                            onSubmit("Player $playerNumber")
                        } else {
                            onSubmit(username)
                        }
                    },
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(0.7f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = MaterialTheme.shapes.medium,
                    enabled = true
                ) {
                    Text(
                        "Start Game",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
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
fun PlayerSwitchDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = { onConfirm() },
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Hand the device to the other player",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(0.7f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "I'm Ready",
                        style = MaterialTheme.typography.titleMedium
                    )
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
        val isShip = isHit && attackedPlayer.ships.any { ship -> coordinate in ship }

        cell.copy(
            // Only show enemy ships if they were hit
            isShip = isShip,
            isHit = isHit,
            isMiss = isMiss
        )
    }
}

private fun calculateShipPositions(startX: Int, startY: Int, isHorizontal: Boolean): List<Pair<Int, Int>> {
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


private fun createInitialBoard(): List<Cell> {
    val board = mutableListOf<Cell>()
    for (y in 0..9) {
        for (x in 0..9) {
            board.add(Cell(x, y))
        }
    }
    return board
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    // Use the correct theme from your code
    BattleshipsTheme {
        // Create a dummy NavController for preview
        val navController = rememberNavController()

        // Use your GameScreen composable directly
        GameScreen(
            modifier = Modifier.padding(16.dp),
            navController = navController
        )
    }
}

