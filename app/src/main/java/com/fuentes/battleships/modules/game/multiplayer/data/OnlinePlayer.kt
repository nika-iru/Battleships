package com.fuentes.battleships.modules.game.multiplayer.data

data class OnlinePlayer(
    val id: String? = null,
    val ships: List<List<Pair<Int, Int>>> = emptyList(), // List of ships, each ship is a list of coordinates
    val hits: List<Pair<Int, Int>> = emptyList(),
    val misses: List<Pair<Int, Int>> = emptyList()
)