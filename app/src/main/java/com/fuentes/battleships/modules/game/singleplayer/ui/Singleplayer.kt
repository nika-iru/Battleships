package com.fuentes.battleships.modules.game.singleplayer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fuentes.battleships.modules.auth.ui.AuthViewModel
import com.fuentes.battleships.modules.game.singleplayer.data.BoardView
import com.fuentes.battleships.modules.game.singleplayer.data.GamePhase
import com.fuentes.battleships.modules.game.singleplayer.data.GameState
import com.fuentes.battleships.modules.game.singleplayer.data.Player
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun SinglePlayerGameScreen(
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
    var isComputerTurn by remember { mutableStateOf(false) }
    var computerThinking by remember { mutableStateOf(false) }

    // Computer's last successful hit for targeting adjacent cells
    var computerLastHit by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var adjacentTargets by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }

    // Computer places ships at the start
    LaunchedEffect(Unit) {
        // Only place computer ships once at the beginning
        if (gameState.player2.ships.isEmpty()) {
            gameState = gameState.copy(
                player2 = placeComputerShips(gameState.player2)
            )
        }
    }

    // Computer's turn logic
    LaunchedEffect(isComputerTurn) {
        if (isComputerTurn && gameState.phase == GamePhase.BATTLE) {
            // Add a short delay to make it seem like the computer is "thinking"
            computerThinking = true
            delay(800L)
            computerThinking = false

            // Computer's attack logic
            val attackCoordinate = if (computerLastHit != null && adjacentTargets.isNotEmpty()) {
                // Target cells adjacent to the last hit
                adjacentTargets.first()
            } else {
                // Random targeting mode
                getRandomUnattackedCoordinate(gameState.player2.hits, gameState.player2.misses)
            }

            // Execute the attack
            handleAttack(
                attackCoordinate.first,
                attackCoordinate.second,
                gameState.copy(isPlayer1Turn = false), // Computer is player 2
                createBoardWithShips(
                    gameState.player1.ships,
                    gameState.player2.hits,
                    gameState.player2.misses
                )
            ) { newState, newBoard, isHit ->
                gameState = newState

                // Update adjacent targets list for computer's next move
                if (isHit) {
                    // Hit! Save this coordinate as last hit
                    computerLastHit = attackCoordinate

                    // Generate adjacent targets if this is the first hit or we've exhausted previous targets
                    if (adjacentTargets.isEmpty() || adjacentTargets.size == 1) {
                        // Create new adjacent targets list
                        val newTargets = getAdjacentCoordinates(attackCoordinate)
                            .filter { (x, y) ->
                                // Filter valid coordinates that haven't been attacked
                                x in 0..9 && y in 0..9 &&
                                        Pair(x, y) !in gameState.player2.hits &&
                                        Pair(x, y) !in gameState.player2.misses
                            }
                        adjacentTargets = newTargets
                    } else {
                        // Remove the used target
                        adjacentTargets = adjacentTargets.filter { it != attackCoordinate }
                    }
                } else {
                    // Miss! Remove this coordinate from adjacent targets if it exists
                    adjacentTargets = adjacentTargets.filter { it != attackCoordinate }

                    // If we've exhausted all adjacent targets, reset to random mode
                    if (adjacentTargets.isEmpty()) {
                        computerLastHit = null
                    }
                }

                // Show the computer's move to the player
                hitMissMessage = if (isHit)
                    "Computer hit your ship at ${('A' + attackCoordinate.first)}${attackCoordinate.second + 1}!"
                else
                    "Computer missed at ${('A' + attackCoordinate.first)}${attackCoordinate.second + 1}."

                showHitMissDialog = true
                isComputerTurn = false

                // Update the board view to show player's board with computer's attacks
                gameBoard = createBoardWithShips(
                    gameState.player1.ships,
                    gameState.player2.hits,
                    gameState.player2.misses
                )
            }
        }
    }

    // Handle game phase transitions
    LaunchedEffect(gameState.phase, gameState.isPlayer1Turn) {
        if (gameState.phase == GamePhase.PLACEMENT && gameState.player1.ships.size == 2 && !isComputerTurn) {
            // Player finished placing ships, transition to battle phase
            gameState = gameState.copy(
                phase = GamePhase.BATTLE,
                isPlayer1Turn = true,
                boardView = BoardView.OPPONENT_BOARD
            )

            showTurnNotification = true
            turnMessage = "Ships placed! Your turn to attack."

            // Set up attack board (showing computer's board to attack)
            gameBoard = createBoardForAttacking(gameState)
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
                    GamePhase.PLACEMENT -> "Place Your Ships (${gameState.player1.ships.size}/2)"
                    GamePhase.BATTLE -> if (computerThinking)
                        "Computer is thinking..."
                    else
                        "Your Turn to Attack"
                    GamePhase.GAME_OVER -> if (gameState.isPlayer1Turn) "You Win!" else "Computer Wins!"
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

            Spacer(modifier = Modifier.height(8.dp))

            // Game Board legend (reusing from GameScreen)
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
                    LegendItem(color = Color(0xFF81D4FA), text = "Water")
                    LegendItem(color = Color(0xFF0C4870), text = "Ship")
                    LegendItem(color = Color(0xFFD32F2F), text = "Hit")
                    LegendItem(color = Color(0xFF9E9E9E), text = "Miss") // Reuse your existing LegendRow composable
                }
            }

            // Board Title
            if (gameState.phase == GamePhase.BATTLE) {
                Text(
                    text = if (gameState.boardView == BoardView.OPPONENT_BOARD)
                        "Computer's Waters (Click to Attack!)"
                    else
                        "Your Waters (Computer's attacks shown)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Game Board (reusing BattleshipGrid from main game)
            BattleshipGrid(
                board = gameBoard,
                enabled = when (gameState.phase) {
                    GamePhase.PLACEMENT -> true
                    GamePhase.BATTLE -> gameState.isPlayer1Turn && !isComputerTurn && !computerThinking
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
                                // Display hit/miss notification
                                hitMissMessage = if (isHit) "Hit! You found an enemy ship!" else "Miss! No ship at this location."
                                showHitMissDialog = true

                                gameState = newState
                                gameBoard = newBoard

                                // Check for game over
                                if (newState.phase == GamePhase.GAME_OVER) {
                                    hitMissMessage = "Game Over! You Win!"
                                } else {
                                    // Set up for computer's turn
                                    isComputerTurn = true
                                }
                            }
                        }
                        GamePhase.GAME_OVER -> { /* Do nothing */ }
                    }
                }
            )

            // Switch view button (your board/computer's board)
            if (gameState.phase == GamePhase.BATTLE && !computerThinking) {
                Button(
                    onClick = {
                        val newView = if (gameState.boardView == BoardView.OPPONENT_BOARD) {
                            // Switch to player's board view
                            BoardView.OWN_BOARD
                        } else {
                            // Switch to opponent's board view
                            BoardView.OPPONENT_BOARD
                        }

                        gameState = gameState.copy(boardView = newView)

                        // Update board based on view
                        gameBoard = if (newView == BoardView.OWN_BOARD) {
                            createBoardWithShips(
                                gameState.player1.ships,
                                gameState.player2.hits,
                                gameState.player2.misses
                            )
                        } else {
                            createBoardForAttacking(gameState)
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(if (gameState.boardView == BoardView.OPPONENT_BOARD) "View Your Board" else "View Computer's Board")
                }
            }

            if (gameState.phase == GamePhase.GAME_OVER) {
                Button(
                    onClick = {
                        // Reset game
                        gameState = GameState()
                        gameBoard = createInitialBoard()
                        computerLastHit = null
                        adjacentTargets = emptyList()
                        isComputerTurn = false

                        // Place computer ships for new game
                        gameState = gameState.copy(
                            player2 = placeComputerShips(gameState.player2)
                        )
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Play Again")
                }
            }
        }
    }

    // Turn notification dialog (reuse your existing dialog)
    if (showTurnNotification) {
        SimpleTurnNotification(
            message = turnMessage,
            onDismiss = {
                showTurnNotification = false
            }
        )
    }

    // Hit/miss dialog (reuse your existing dialog)
    if (showHitMissDialog) {
        HitMissDialog(
            message = hitMissMessage,
            onDismiss = {
                showHitMissDialog = false
            }
        )
    }
}

// Get a list of adjacent coordinates (up, down, left, right)
private fun getAdjacentCoordinates(coordinate: Pair<Int, Int>): List<Pair<Int, Int>> {
    val (x, y) = coordinate
    return listOf(
        Pair(x - 1, y), // Left
        Pair(x + 1, y), // Right
        Pair(x, y - 1), // Up
        Pair(x, y + 1)  // Down
    )
}

// Get a random unattacked coordinate for computer targeting
private fun getRandomUnattackedCoordinate(hits: List<Pair<Int, Int>>, misses: List<Pair<Int, Int>>): Pair<Int, Int> {
    val attackedCoordinates = hits + misses
    var randomCoordinate: Pair<Int, Int>

    do {
        val x = Random.nextInt(0, 10)
        val y = Random.nextInt(0, 10)
        randomCoordinate = Pair(x, y)
    } while (randomCoordinate in attackedCoordinates)

    return randomCoordinate
}

// Place computer ships randomly
private fun placeComputerShips(player: Player): Player {
    val newShips = mutableListOf<List<Pair<Int, Int>>>()

    while (newShips.size < 2) { // Place 2 ships
        val isHorizontal = Random.nextBoolean()
        val maxX = if (isHorizontal) 7 else 9 // Leave room for ship length
        val maxY = if (isHorizontal) 9 else 7

        val startX = Random.nextInt(0, maxX + 1)
        val startY = Random.nextInt(0, maxY + 1)

        val shipPositions = calculateShipPositions(startX, startY, isHorizontal)

        // Check for overlaps with existing ships
        val existingPositions = newShips.flatten()
        if (shipPositions.none { it in existingPositions }) {
            newShips.add(shipPositions)
        }
    }

    return player.copy(ships = newShips)
}