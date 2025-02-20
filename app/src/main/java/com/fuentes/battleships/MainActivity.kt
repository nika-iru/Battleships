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
import com.fuentes.battleships.models.data.Tile.Tile
import com.fuentes.battleships.models.data.Grid.Grid
import com.fuentes.battleships.models.data.Ships.MainShip
import com.fuentes.battleships.models.data.Tile.TileState
import com.fuentes.battleships.models.data.Ships.Orientation
import com.fuentes.battleships.ui.theme.BattleshipsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BattleshipsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BattleshipGame()
                }
            }
        }
    }
}
@Composable
fun BattleshipGame() {
    var player1Name by remember { mutableStateOf("") }
    var player2Name by remember { mutableStateOf("") }
    var namesEntered by remember { mutableStateOf(false) }

    val player1Grid by remember { mutableStateOf(Grid("Player 1 Grid")) }
    val player2Grid by remember { mutableStateOf(Grid("Player 2 Grid")) }

    var player1EliminatedShips by remember { mutableIntStateOf(0) }
    var player2EliminatedShips by remember { mutableIntStateOf(0) }

    var isGameOver by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var placingShips by remember { mutableStateOf(true) }
    var placedShips by remember { mutableIntStateOf(0) }
    var currentPlayer by remember { mutableIntStateOf(1) }

    if (!namesEntered) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("Welcome to Battleship", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = player1Name,
                    onValueChange = { player1Name = it },
                    label = { Text("Player 1 Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = player2Name,
                    onValueChange = { player2Name = it },
                    label = { Text("Player 2 Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    if (player1Name.isNotBlank() && player2Name.isNotBlank()) {
                        namesEntered = true
                        message = "$player1Name, place your ships."
                    }
                },
                modifier = Modifier.padding(top = 16.dp),
                enabled = player1Name.isNotBlank() && player2Name.isNotBlank()
            ) {
                Text("Start Game")
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Battleship Game", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Text("Current Turn: ${if (currentPlayer == 1) player1Name else player2Name}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BattleshipGrid(
                    board = if (currentPlayer == 1) player1Grid.tiles else player2Grid.tiles,
                    onCellClick = { x, y ->
                        if (!isGameOver) {
                            if (placingShips) {
                                val playerShip = MainShip(length = 3, orientation = Orientation.HORIZONTAL)
                                val grid = if (currentPlayer == 1) player1Grid else player2Grid
                                if (grid.placeShip(playerShip, x, y)) {
                                    placedShips++
                                    message = "Ship placed. Place another one."
                                    if (placedShips == 3) {
                                        if (currentPlayer == 1) {
                                            currentPlayer = 2
                                            placedShips = 0
                                            message = "$player2Name, place your ships."
                                        } else {
                                            placingShips = false
                                            message = "All ships placed! $player1Name, start attacking."
                                            currentPlayer = 1
                                        }
                                    }
                                } else {
                                    message = "Invalid ship placement. Try again."
                                }
                            } else {
                                val opponentGrid = if (currentPlayer == 1) player2Grid else player1Grid
                                val hitShip = opponentGrid.attackTile(x, y)

                                if (hitShip) {
                                    message = "${
                                        if (currentPlayer == 1) player1Name else player2Name
                                    } hit ${
                                        if (currentPlayer == 1) player2Name else player1Name
                                    }'s ship!"

                                    val eliminatedCount = opponentGrid.eliminatedShips.size


                                    if (currentPlayer == 1 && eliminatedCount > player2EliminatedShips) {
                                        player2EliminatedShips = eliminatedCount
                                        message = "$player1Name eliminated one of $player2Name's ships! (${player2EliminatedShips}/3)"

                                        if (player2EliminatedShips == 3) {
                                            isGameOver = true
                                            message = "Game Over! $player1Name Wins!"
                                        }
                                    } else if (currentPlayer == 2 && eliminatedCount > player1EliminatedShips) {
                                        player1EliminatedShips = eliminatedCount
                                        message = "$player2Name eliminated one of $player1Name's ships! (${player1EliminatedShips}/3)"

                                        if (player1EliminatedShips == 3) {
                                            isGameOver = true
                                            message = "Game Over! $player2Name Wins!"
                                        }
                                    }

                                    if (!isGameOver) {
                                        currentPlayer = if (currentPlayer == 1) 2 else 1
                                    }
                                } else {
                                    message = "${
                                        if (currentPlayer == 1) player1Name else player2Name
                                    } missed!"
                                    currentPlayer = if (currentPlayer == 1) 2 else 1
                                }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
            )
        }
    }
}



@Composable
fun BattleshipGrid(board: List<Tile>, onCellClick: (Int, Int) -> Unit) {
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
                onClick = { onCellClick(tile.coord.first, tile.coord.second) },
                enabled = tile.state == TileState.UNKNOWN || tile.state == TileState.SHIP
            )
        }
    }
}

@Composable
fun BattleshipCell(tile: Tile, onClick: () -> Unit, enabled: Boolean) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(32.dp).aspectRatio(1f),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = tile.state.color  // ✅ Use the enum’s color property directly
        )
    ) {}
}
