package com.fuentes.battleships.modules.game.ui

import com.fuentes.battleships.modules.game.data.models.BoardView
import com.fuentes.battleships.modules.game.data.models.Cell
import com.fuentes.battleships.modules.game.data.models.GamePhase
import com.fuentes.battleships.modules.game.data.models.GameSession

data class GameState(
    val player1: Player = Player(), // Firebase Auth User ID
    val player2: Player = Player(), // Firebase Auth User ID
    val player1Ships: List<List<Pair<Int, Int>>> = emptyList(),
    val player2Ships: List<List<Pair<Int, Int>>> = emptyList(),
    val player1Hits: List<Pair<Int, Int>> = emptyList(),
    val player2Hits: List<Pair<Int, Int>> = emptyList(),
    val player1Misses: List<Pair<Int, Int>> = emptyList(),
    val player2Misses: List<Pair<Int, Int>> = emptyList(),
    val isPlayer1Turn: Boolean = true,
    val phase: GamePhase = GamePhase.PLACEMENT,
    val isHorizontal: Boolean = true,
    val boardView: BoardView = BoardView.OWN_BOARD,
    val timer: Int = 15
)