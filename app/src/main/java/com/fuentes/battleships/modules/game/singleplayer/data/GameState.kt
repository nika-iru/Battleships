package com.fuentes.battleships.modules.game.singleplayer.data

data class GameState(
    val player1: String? = null, // Firebase Auth User ID
    val player2: String? = null, // Firebase Auth User ID
    val player1Ships: List<List<Pair<Int, Int>>> = emptyList(),
    val player2Ships: List<List<Pair<Int, Int>>> = emptyList(),
    val player1Hits: List<Pair<Int, Int>> = emptyList(),
    val player2Hits: List<Pair<Int, Int>> = emptyList(),
    val player1Misses: List<Pair<Int, Int>> = emptyList(),
    val player2Misses: List<Pair<Int, Int>> = emptyList(),
    val isPlayer1Turn: Boolean = true,
    val phase: Int = 0,
    val isHorizontal: Boolean = true,
    val boardView: Int = 0,
    val timer: Int = 15
)