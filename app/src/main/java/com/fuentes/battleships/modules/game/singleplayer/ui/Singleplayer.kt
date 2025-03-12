package com.fuentes.battleships.modules.game.singleplayer.ui

import android.util.Log
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
import com.fuentes.battleships.modules.auth.ui.User
import com.fuentes.battleships.modules.game.GameLogic
import com.fuentes.battleships.modules.game.singleplayer.data.GameState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay

@Composable
fun SinglePlayerGameScreen(
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
    var isComputerTurn by remember { mutableStateOf(false) }
    var computerThinking by remember { mutableStateOf(false) }

    // Computer's last successful hit for targeting adjacent cells
    var computerLastHit by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var adjacentTargets by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }

    var db = Firebase.firestore
    var currentUser = Firebase.auth.currentUser?.uid
    val userDocRef = db.collection("Users").document(currentUser.toString())
    // Computer places ships at the start
    LaunchedEffect(Unit) {
        // Only place computer ships once at the beginning
        if (gameState.player2Ships.isEmpty()) {
            gameState = gameState.copy(
                player2Ships = gameLogic.placeComputerShips()
            )
        }
    }

    // Computer's turn logic
    LaunchedEffect(isComputerTurn) {
        if (isComputerTurn && gameState.phase == 1) {
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
                gameLogic.getRandomUnattackedCoordinate(gameState.player2Hits, gameState.player2Misses)
            }

            // Execute the attack
            gameLogic.handleAttack(
                attackCoordinate.first,
                attackCoordinate.second,
                gameState.copy(isPlayer1Turn = false), // Computer is player 2
                gameLogic.createBoardWithShips(
                    gameState.player1Ships,
                    gameState.player2Hits,
                    gameState.player2Misses
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
                        val newTargets = gameLogic.getAdjacentCoordinates(attackCoordinate)
                            .filter { (x, y) ->
                                // Filter valid coordinates that haven't been attacked
                                x in 0..9 && y in 0..9 &&
                                        Pair(x, y) !in gameState.player2Hits &&
                                        Pair(x, y) !in gameState.player2Misses
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
                gameBoard = gameLogic.createBoardWithShips(
                    gameState.player1Ships,
                    gameState.player2Hits,
                    gameState.player2Misses
                )
            }
        }
    }

    // Handle game phase transitions
    LaunchedEffect(gameState.phase, gameState.isPlayer1Turn) {
        if (gameState.phase == 0 && gameState.player1Ships.size == 2 && !isComputerTurn) {
            // Player finished placing ships, transition to battle phase
            gameState = gameState.copy(
                phase = 1,
                isPlayer1Turn = true,
                boardView = 1
            )

            showTurnNotification = true
            turnMessage = "Ships placed! Your turn to attack."

            // Set up attack board (showing computer's board to attack)
            gameBoard = gameLogic.createBoardForAttacking(gameState)
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
                    0 -> "Place Your Ships (${gameState.player1Ships.size}/2)"
                    1 -> if (computerThinking)
                        "Computer is thinking..."
                    else
                        "Your Turn to Attack"
                    else -> if (gameState.isPlayer1Turn) "You Win!" else "Computer Wins!"
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
            if (gameState.phase == 1) {
                Text(
                    text = if (gameState.boardView == 1)
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
                    0 -> true
                    1 -> gameState.isPlayer1Turn && !isComputerTurn && !computerThinking
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

                                    hitMissMessage = "Game Over! You Win!"

                                    userDocRef.get().addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            val currentWins = document.getLong("wins") ?: 0
                                            userDocRef.update("wins", currentWins + 1)
                                                .addOnSuccessListener {
                                                    Log.d("Firestore", "Wins updated successfully")
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("Firestore", "Error updating wins", e)
                                                }
                                        } else {
                                            // If user document doesn't exist, create it with wins = 1
                                            userDocRef.set(mapOf("wins" to 1))
                                                .addOnSuccessListener {
                                                    Log.d("Firestore", "User document created with wins = 1")
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("Firestore", "Error creating user document", e)
                                                }
                                        }
                                    }.addOnFailureListener { e ->
                                        Log.e("Firestore", "Error retrieving user document", e)
                                    }
                                } else {
                                    // Set up for computer's turn
                                    isComputerTurn = true
                                }
                            }
                        }
                        2 -> { /* Do nothing */ }
                    }
                }
            )

            // Switch view button (your board/computer's board)
            if (gameState.phase == 1 && !computerThinking) {
                Button(
                    onClick = {
                        val newView = if (gameState.boardView == 1) {
                            // Switch to player's board view
                            0
                        } else {
                            // Switch to opponent's board view
                            1
                        }

                        gameState = gameState.copy(boardView = newView)

                        // Update board based on view
                        gameBoard = if (newView == 0) {
                            gameLogic.createBoardWithShips(
                                gameState.player1Ships,
                                gameState.player2Hits,
                                gameState.player2Misses
                            )
                        } else {
                            gameLogic.createBoardForAttacking(gameState)
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(if (gameState.boardView == 1) "View Your Board" else "View Computer's Board")
                }
            }

            if (gameState.phase == 2) {
                Button(
                    onClick = {
                        // Reset game
                        gameState = GameState()
                        gameBoard = gameLogic.createInitialBoard()
                        computerLastHit = null
                        adjacentTargets = emptyList()
                        isComputerTurn = false

                        // Place computer ships for new game
                        gameState = gameState.copy(
                            player2Ships = gameLogic.placeComputerShips()
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

