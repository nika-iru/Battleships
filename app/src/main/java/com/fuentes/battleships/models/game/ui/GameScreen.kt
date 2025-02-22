package com.fuentes.battleships.models.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fuentes.battleships.models.auth.ui.AuthViewModel
import com.fuentes.battleships.models.game.data.Cell
import com.fuentes.battleships.ui.theme.BattleshipsTheme

data class Cell(
    val x: Int,
    val y: Int,
    var isShip: Boolean = false,  // Added to show ship placement
    var isHit: Boolean = false,
    var isMiss: Boolean = false
)

data class Player(
    val ships: List<List<Pair<Int, Int>>> = emptyList(), // List of ships, each ship is a list of coordinates
    val hits: List<Pair<Int, Int>> = emptyList(),
    val misses: List<Pair<Int, Int>> = emptyList()
)

data class GameState(
    val player1: Player = Player(),
    val player2: Player = Player(),
    val isPlayer1Turn: Boolean = true,
    val phase: GamePhase = GamePhase.PLACEMENT,
    val isHorizontal: Boolean = true
)

enum class GamePhase {
    PLACEMENT,
    BATTLE,
    GAME_OVER
}

// Previous data classes remain the same

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    var gameBoard by remember { mutableStateOf(createInitialBoard()) }
    var gameState by remember { mutableStateOf(GameState()) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (gameState.phase) {
                    GamePhase.PLACEMENT -> {
                        val currentPlayer = if (gameState.isPlayer1Turn) gameState.player1 else gameState.player2
                        "Player ${if (gameState.isPlayer1Turn) "1" else "2"} Place Your Ship (${currentPlayer.ships.size}/2)"
                    }
                    GamePhase.BATTLE -> "Player ${if (gameState.isPlayer1Turn) "1" else "2"}'s Turn to Attack"
                    GamePhase.GAME_OVER -> "Game Over! ${if (gameState.isPlayer1Turn) "Player 2" else "Player 1"} Wins!"
                },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (gameState.phase == GamePhase.PLACEMENT) {
                Button(
                    onClick = {
                        gameState = gameState.copy(isHorizontal = !gameState.isHorizontal)
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(if (gameState.isHorizontal) "Horizontal" else "Vertical")
                }
            }

            BattleshipGrid(
                board = gameBoard,
                onCellClick = { x, y ->
                    when (gameState.phase) {
                        GamePhase.PLACEMENT -> handlePlacement(x, y, gameState, gameBoard) { newState, newBoard ->
                            gameState = newState
                            gameBoard = newBoard
                        }
                        GamePhase.BATTLE -> handleAttack(x, y, gameState, gameBoard) { newState, newBoard ->
                            gameState = newState
                            // After attack, automatically show the next player's board
                            if (newState.phase != GamePhase.GAME_OVER && newState.isPlayer1Turn != gameState.isPlayer1Turn) {
                                gameBoard = createBoardWithShips(
                                    if (newState.isPlayer1Turn) newState.player1.ships else newState.player2.ships
                                )
                            } else {
                                gameBoard = newBoard
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
}

@Composable
fun BattleshipCell(
    cell: Cell,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(32.dp)
            .aspectRatio(1f),
        enabled = !cell.isHit && !cell.isMiss,
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                cell.isHit -> Color.Red
                cell.isMiss -> Color.Gray
                cell.isShip -> Color.Blue  // Show ships in blue
                else -> MaterialTheme.colorScheme.primary
            }
        )
    ) {}
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
                        isPlayer1Turn = true
                    )
                    // Show player 1's ships for their turn
                    onUpdate(newState, createBoardWithShips(newState.player1.ships))
                } else {
                    // Switch to player 2's placement
                    newState = newState.copy(isPlayer1Turn = false)
                    onUpdate(newState, createInitialBoard())
                }
            } else {
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
    onUpdate: (GameState, List<Cell>) -> Unit
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
                cell.copy(isHit = isHit, isMiss = !isHit)
            } else cell
        }

        var newState = if (gameState.isPlayer1Turn) {
            gameState.copy(player1 = newAttackingPlayer)
        } else {
            gameState.copy(player2 = newAttackingPlayer)
        }

        // Check for game over
        newState = if (newAttackingPlayer.hits.size == 6) { // 2 ships × 3 cells
            newState.copy(phase = GamePhase.GAME_OVER)
        } else {
            // Switch turns
            newState.copy(isPlayer1Turn = !newState.isPlayer1Turn)
        }

        onUpdate(newState, newBoard)
    }
}

private fun createBoardWithShips(ships: List<List<Pair<Int, Int>>>): List<Cell> {
    return createInitialBoard().map { cell ->
        cell.copy(
            isShip = ships.any { ship ->
                ship.any { (shipX, shipY) -> shipX == cell.x && shipY == cell.y }
            }
        )
    }
}

// Your existing calculateShipPositions, BattleshipGrid, and createInitialBoard functions remain the same

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
    onCellClick: (Int, Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(10),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(100) { index ->
            val cell = board[index]
            BattleshipCell(
                cell = cell,
                onClick = { onCellClick(cell.x, cell.y) }
            )
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