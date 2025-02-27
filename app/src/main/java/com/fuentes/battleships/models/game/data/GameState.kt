package com.fuentes.battleships.models.game.data

data class GameState(
    val player1: Player = Player(),
    val player2: Player = Player(),
    val isPlayer1Turn: Boolean = true,
    val phase: GamePhase = GamePhase.PLACEMENT,
    val isHorizontal: Boolean = true,
    val boardView: BoardView = BoardView.OWN_BOARD
)