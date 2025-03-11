package com.fuentes.battleships.modules.game.singleplayer.data

data class Player(
    val ships: List<List<Pair<Int, Int>>> = emptyList(), // List of ships, each ship is a list of coordinates
    val hits: List<Pair<Int, Int>> = emptyList(),
    val misses: List<Pair<Int, Int>> = emptyList()
)