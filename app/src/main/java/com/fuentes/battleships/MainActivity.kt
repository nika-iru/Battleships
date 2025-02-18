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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fuentes.battleships.ui.theme.BattleshipsTheme
import com.fuentes.battleships.models.data.Cell

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
    var gameBoard by remember { mutableStateOf(createInitialBoard()) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Battleship",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            BattleshipGrid(
                board = gameBoard,
                onCellClick = { x, y ->
                    gameBoard = gameBoard.map { cell ->
                        if (cell.x == x && cell.y == y) {
                            if (cell.isShip) {
                                cell.copy(isHit = true)
                            } else {
                                cell.copy(isMiss = true)
                            }
                        } else cell
                    }
                }
            )
        }
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
                else -> MaterialTheme.colorScheme.primary
            }
        )
    ) {
        // Cell content can be added here if needed
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

@Preview
@Composable
fun BattleshipGamePreview() {
    BattleshipsTheme {
        BattleshipGame()
    }
}