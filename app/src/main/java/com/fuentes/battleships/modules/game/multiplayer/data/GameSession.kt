package com.fuentes.battleships.modules.game.multiplayer.data

data class GameSession(
    val sessionId: String? = null,
    val player1Id: String? = null, // Firebase Auth User ID
    val player2Id: String? = null, // Firebase Auth User ID
    val status: String = "waiting", // waiting, playing, finished
    val player1Ships: List<List<Int>> = emptyList(),
    val player2Ships: List<List<Int>> = emptyList(),
    val player1Hits: List<Int> = emptyList(),
    val player2Hits: List<Int> = emptyList(),
    val player1Misses: List<Int> = emptyList(),
    val player2Misses: List<Int> = emptyList(),
    val isPlayer1Turn: Boolean = true,
    val phase: Int = 0, //0 = Placement, 1 = battle, 2 = game over
    val isHorizontal: Boolean = true,
    val boardView: Int = 0,// 0 = own board, 1 = opponent board
    val timer: Int = 15,
    val winnerId: String? = null,
    val showTurnNotification: Boolean = false,
    val turnMessage: String = ""
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "sessionId" to sessionId,
            "player1Id" to player1Id,
            "player2Id" to player2Id,
            "status" to status,
            "player1Ships" to player1Ships.flatten(),
            "player2Ships" to player2Ships.flatten(),
            "player1Hits" to player1Hits,
            "player2Hits" to player2Hits,
            "player1Misses" to player1Misses,
            "player2Misses" to player2Misses,
            "isPlayer1Turn" to isPlayer1Turn,
            "isHorizontal" to isHorizontal,
            "phase" to phase,
            "timer" to timer,
            "showTurnNotification" to showTurnNotification,
            "turnMessage" to turnMessage
        )
    }
}

data class GameList(
    val sessionId: String? = null,
    val player1Id: String? = null
)