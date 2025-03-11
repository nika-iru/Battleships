package com.fuentes.battleships.modules.game.multiplayer.data

data class GameSession(
    val sessionId: String? = null,
    val player1Id: String? = null, // Firebase Auth User ID
    val player2Id: String? = null, // Firebase Auth User ID
    val status: String = "waiting", // waiting, playing, finished
    val player1Ships: List<List<Pair<Int, Int>>> = emptyList(),
    val player2Ships: List<List<Pair<Int, Int>>> = emptyList(),
    val player1Hits: List<Pair<Int, Int>> = emptyList(),
    val player2Hits: List<Pair<Int, Int>> = emptyList(),
    val player1Misses: List<Pair<Int, Int>> = emptyList(),
    val player2Misses: List<Pair<Int, Int>> = emptyList(),
    val isPlayer1Turn: Boolean = true,
    val phase: Int = 0, //0 = Placement, 1 = battle, 2 = game over
    val isHorizontal: Boolean = true,
    val boardView: Int = 0,// 0 = own board, 1 = opponent board
    val timer: Int = 15,
    val winnerId: String? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "sessionId" to sessionId,
            "player1Id" to player1Id,
            "player2Id" to player2Id,
            "status" to status,
            "player1Ships" to player1Ships.map { ship -> ship.map { listOf(it.first, it.second) } },
            "player2Ships" to player2Ships.map { ship -> ship.map { listOf(it.first, it.second) } },
            "player1Hits" to player1Hits.map { listOf(it.first, it.second) },
            "player2Hits" to player2Hits.map { listOf(it.first, it.second) },
            "player1Misses" to player1Misses.map { listOf(it.first, it.second) },
            "player2Misses" to player2Misses.map { listOf(it.first, it.second) },
            "isPlayer1Turn" to isPlayer1Turn,
            "isHorizontal" to isHorizontal,
            "phase" to phase,
            "timer" to timer
        )
        // when reading do this
        //val player1Hits = (snapshot["player1Hits"] as? List<List<Int>>)?.map { Pair(it[0], it[1]) } ?: emptyList()
        //val player2Hits = (snapshot["player2Hits"] as? List<List<Int>>)?.map { Pair(it[0], it[1]) } ?: emptyList()
    }
}

data class GameList(
    val sessionId: String? = null,
    val player1Id: String? = null
)