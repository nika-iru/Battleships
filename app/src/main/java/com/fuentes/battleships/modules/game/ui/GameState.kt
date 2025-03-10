package com.fuentes.battleships.modules.game.ui

import com.fuentes.battleships.modules.game.data.models.BoardView
import com.fuentes.battleships.modules.game.data.models.Cell
import com.fuentes.battleships.modules.game.data.models.GamePhase
import com.fuentes.battleships.modules.game.data.models.GameSession

data class GameState(
    val gameSession: GameSession? = null, // Current game session
    val localBoard: List<Cell> = createInitialBoard(), // Local UI board
    val showHitMissDialog: Boolean = false,
    val hitMissMessage: String = "",
    val showTurnNotification: Boolean = false,
    val turnMessage: String = "",
    val isTimerRunning: Boolean = false
)