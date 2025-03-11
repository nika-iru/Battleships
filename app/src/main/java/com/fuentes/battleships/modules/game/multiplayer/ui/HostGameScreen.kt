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
import com.fuentes.battleships.modules.game.singleplayer.data.Cell
import com.fuentes.battleships.modules.game.singleplayer.data.GamePhase
import com.fuentes.battleships.modules.game.singleplayer.data.GameState
import com.fuentes.battleships.modules.game.singleplayer.data.Player
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun HostGameScreen(
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

    // Handle game phase transitions
    LaunchedEffect(gameState.phase, gameState.isPlayer1Turn) {
        if (gameState.phase == GamePhase.PLACEMENT && gameState.player1.ships.size == 2 && gameState.player2.ships.size == 2) {
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
                    GamePhase.BATTLE -> "Your Turn to Attack"
                    GamePhase.GAME_OVER -> if (gameState.isPlayer1Turn) "You Win!" else "You Lose!"
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
                    GamePhase.BATTLE -> gameState.isPlayer1Turn
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
                                    // Set up go to enemy's turn
                                    gameState = gameState.copy(isPlayer1Turn = false)
                                }
                            }
                        }
                        GamePhase.GAME_OVER -> { /* Do nothing */ }
                    }
                }
            )

            // Switch view button (your board/computer's board)
            if (gameState.phase == GamePhase.BATTLE && gameState.isPlayer1Turn) {
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
                    Text(if (gameState.boardView == BoardView.OPPONENT_BOARD) "View Your Board" else "View Enemy's Board")
                }
            }

            if (gameState.phase == GamePhase.GAME_OVER) {
                Button(
                    onClick = {
                        // Reset game
                        gameState = GameState()
                        gameBoard = createInitialBoard()

                        gameState = gameState.copy(
                            //start a new game
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