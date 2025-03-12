package com.fuentes.battleships.modules.game.singleplayer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fuentes.battleships.modules.auth.ui.AuthViewModel
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

    var computerLastHit by remember { mutableStateOf<Int?>(null) }
    var adjacentTargets by remember { mutableStateOf(listOf<Int>()) }

    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser?.uid
    val userDocRef = db.collection("Users").document(currentUser.toString())

    LaunchedEffect(Unit) {
        if (gameState.player2Ships.isEmpty()) {
            gameState = gameState.copy(player2Ships = gameLogic.placeComputerShips())
        }
    }

    LaunchedEffect(isComputerTurn) {
        if (isComputerTurn && gameState.phase == 1) {
            computerThinking = true
            delay(800L)
            computerThinking = false

            val attackIndex =
                adjacentTargets.firstOrNull() ?: gameLogic.getRandomUnattackedCoordinate(
                    gameState.player2Hits, gameState.player2Misses
                )

            gameLogic.handleAttack(
                attackIndex,
                gameState.copy(isPlayer1Turn = false),
                gameLogic.createBoardWithShips(
                    gameState.player1Ships,
                    gameState.player2Hits,
                    gameState.player2Misses
                )
            ) { newState, newBoard, isHit ->
                gameState = newState
                gameBoard = newBoard

                if (isHit) {
                    computerLastHit = attackIndex
                    adjacentTargets = gameLogic.getAdjacentCoordinates(attackIndex)
                        .filter { it !in gameState.player2Hits && it !in gameState.player2Misses }
                } else {
                    adjacentTargets = adjacentTargets.filter { it != attackIndex }
                    if (adjacentTargets.isEmpty()) computerLastHit = null
                }

                hitMissMessage = if (isHit) "Computer hit your ship at index $attackIndex!"
                else "Computer missed at index $attackIndex."

                showHitMissDialog = true
                isComputerTurn = false
            }
        }
    }

    LaunchedEffect(gameState.phase, gameState.isPlayer1Turn) {
        if (gameState.phase == 0 && gameState.player1Ships.size == 2 && !isComputerTurn) {
            gameState = gameState.copy(phase = 1, isPlayer1Turn = true, boardView = 1)
            showTurnNotification = true
            turnMessage = "Ships placed! Your turn to attack."
            gameBoard = gameLogic.createBoardForAttacking(gameState)
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = when (gameState.phase) {
                    0 -> "Place Your Ships (${gameState.player1Ships.size}/2)"
                    1 -> if (computerThinking) "Computer is thinking..." else "Your Turn to Attack"
                    else -> if (gameState.isPlayer1Turn) "You Win!" else "Computer Wins!"
                },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

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

            if (gameState.phase == 1) {
                Text(
                    text = if (gameState.boardView == 1) "Computer's Waters (Click to Attack!)"
                    else "Your Waters (Computer's attacks shown)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            BattleshipGrid(
                board = gameBoard,
                enabled = gameState.phase == 0 || (gameState.phase == 1 && gameState.isPlayer1Turn && !isComputerTurn && !computerThinking),
                onCellClick = { index ->
                    when (gameState.phase) {
                        0 -> gameLogic.handlePlacement(
                            index,
                            gameState,
                            gameBoard
                        ) { newState, newBoard ->
                            gameState = newState
                            gameBoard = newBoard
                        }

                        1 -> gameLogic.handleAttack(
                            index,
                            gameState,
                            gameBoard
                        ) { newState, newBoard, isHit ->
                            hitMissMessage =
                                if (isHit) "Hit! You found an enemy ship!" else "Miss! No ship at this location."
                            showHitMissDialog = true
                            gameState = newState
                            gameBoard = newBoard
                            if (newState.phase == 2) hitMissMessage =
                                "Game Over! You Win!" else isComputerTurn = true
                        }

                        2 -> {}
                    }
                }
            )

            if (gameState.phase == 1 && !computerThinking) {
                Button(
                    onClick = {
                        gameState =
                            gameState.copy(boardView = if (gameState.boardView == 1) 0 else 1)
                        gameBoard = if (gameState.boardView == 0)
                            gameLogic.createBoardWithShips(
                                gameState.player1Ships,
                                gameState.player2Hits,
                                gameState.player2Misses
                            )
                        else gameLogic.createBoardForAttacking(gameState)
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(if (gameState.boardView == 1) "View Your Board" else "View Computer's Board")
                }
            }
        }
    }

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


