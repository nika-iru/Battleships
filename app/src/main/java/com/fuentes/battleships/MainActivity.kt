package com.fuentes.battleships

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fuentes.battleships.models.data.logic.Grid
import com.fuentes.battleships.models.data.logic.MainShip
import com.fuentes.battleships.models.data.logic.TileState
import com.fuentes.battleships.models.data.logic.Orientation
import com.fuentes.battleships.ui.theme.BattleshipsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // Allows edge-to-edge display
        setContent {
            BattleshipsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BattleshipGame() // Start the Battleship game
                }
            }
        }
    }
}

@Composable
fun BattleshipGame() {
    val player1Grid = remember { mutableStateOf(Grid("Player 1 Grid")) }
    val player2Grid = remember { mutableStateOf(Grid("Player 2 Grid")) }
    var isGameOver by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Player 1, place your ships.") }
    var placingShips by remember { mutableStateOf(true) }
    var placedShips by remember { mutableIntStateOf(0) }
    var currentPlayer by remember { mutableIntStateOf(1) }

    Surface(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Title and message
            Text(
                text = "Welcome to Battleship",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Display the current message
            Text(
                text = message,
                color = Color.Black,
                modifier = Modifier.padding(top = 16.dp)
            )

            if (isGameOver) {
                Text(
                    text = "Game Over! ${if (player2Grid.value.allShipsSunk()) "Player 1 Wins!" else "Player 2 Wins!"}",
                    color = Color.Red
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BattleshipGrid(
                        board = if (currentPlayer == 1) player1Grid.value.tiles else player2Grid.value.tiles,
                        onCellClick = { x, y ->
                            // Handle ship placement or attack based on the current state
                            if (placingShips) {
                                val playerShip = MainShip(length = 3, orientation = Orientation.HORIZONTAL)
                                val gridState = if (currentPlayer == 1) player1Grid else player2Grid
                                val grid = gridState.value

                                // Attempt to place the ship and update the grid immediately
                                if (grid.placeShip(playerShip, x, y)) {
                                    gridState.value = grid // Update the grid
                                    placedShips++ // Increment the number of ships placed
                                    message = "Ship placed. Place another one."

                                    if (placedShips == 3) {
                                        if (currentPlayer == 1) {
                                            currentPlayer = 2
                                            placedShips = 0
                                            message = "Player 2, place your ships."
                                        } else {
                                            placingShips = false
                                            message = "All ships placed! Player 1, start attacking."
                                            currentPlayer = 1
                                        }
                                    }
                                } else {
                                    message = "Invalid ship placement. Try again."
                                }
                            } else {
                                val opponentGridState = if (currentPlayer == 1) player2Grid else player1Grid
                                val opponentGrid = opponentGridState.value

                                if (!isGameOver) {
                                    if (opponentGrid.attackTile(x, y)) {
                                        opponentGridState.value = opponentGrid

                                        // Check if the ship is hit
                                        message = "Player $currentPlayer hit Player ${if (currentPlayer == 1) 2 else 1}'s ship!"

                                        // Now check if the ship is sunk after the hit
                                        val shipSunk = opponentGrid.ships.any { it.isSunk(opponentGrid) }
                                        if (shipSunk) {
                                            message = "Player $currentPlayer sunk Player ${if (currentPlayer == 1) 2 else 1}'s ship!"
                                        }

                                        // Check if all ships are sunk to end the game
                                        if (opponentGrid.allShipsSunk()) {
                                            isGameOver = true
                                            message = "Game Over! Player $currentPlayer Wins!"
                                        } else {
                                            // Change player if the game is not over
                                            currentPlayer = if (currentPlayer == 1) 2 else 1
                                        }
                                    } else {
                                        // If the shot misses
                                        message = "Player $currentPlayer missed!"
                                        currentPlayer = if (currentPlayer == 1) 2 else 1
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BattleshipGrid(
    board: List<com.fuentes.battleships.models.data.logic.Tile>,
    onCellClick: (Int, Int) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(10),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.wrapContentSize()
        ) {
            items(board.size) { index ->
                val tile = board[index]
                BattleshipCell(
                    tile = tile,
                    onClick = { onCellClick(tile.coord.first, tile.coord.second) }
                )
            }
        }
    }
}

@Composable
fun BattleshipCell(tile: com.fuentes.battleships.models.data.logic.Tile, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(32.dp).aspectRatio(1f),
        enabled = tile.state == TileState.UNKNOWN || tile.state == TileState.SHIP,
        colors = ButtonDefaults.buttonColors(
            containerColor = when (tile.state) {
                TileState.HIT -> Color.Red    // Color for HIT tiles
                TileState.MISS -> Color.Gray  // Color for MISS tiles
                TileState.SUNK -> Color.Black  // Color for SUNK ships
                TileState.SHIP -> Color.Green  // Color for tiles with ships
                else -> MaterialTheme.colorScheme.primary  // Default color for unknown state
            }
        )
    ) {}
}
