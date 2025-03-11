package com.fuentes.battleships.modules.game.multiplayer.data

import com.fuentes.battleships.modules.game.singleplayer.data.BoardView
import com.fuentes.battleships.modules.game.singleplayer.data.GamePhase

data class GameSession(
    val id: String? = null,
    val player1Id: String? = null, // Firebase Auth User ID
    val player2Id: String? = null, // Firebase Auth User ID
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
    val timer: Int = 15,
    val winnerId: String? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "player1Id" to player1Id,
            "player2Id" to player2Id,
            "player1Ships" to player1Ships,
            "player2Ships" to player2Ships,
            "player1Hits" to player1Hits,
            "player2Hits" to player2Hits,
            "player1Misses" to player1Misses,
            "player2Misses" to player2Misses,
            "isPlayer1Turn" to isPlayer1Turn,
            "phase" to phase,
            "isHorizontal" to isHorizontal,
            "boardView" to boardView,
            "timer" to timer
        )
    }
}