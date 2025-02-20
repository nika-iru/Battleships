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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fuentes.battleships.models.auth.ui.AuthViewModel
import com.fuentes.battleships.models.game.data.Cell
import com.fuentes.battleships.ui.theme.BattleshipsTheme

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    navController: NavController
) {
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