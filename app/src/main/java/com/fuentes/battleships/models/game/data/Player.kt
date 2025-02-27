package com.fuentes.battleships.models.game.data

data class Player(
    val username: String = "",
    val ships: List<List<Pair<Int, Int>>> = emptyList(), // List of ships, each ship is a list of coordinates
    val hits: List<Pair<Int, Int>> = emptyList(),
    val misses: List<Pair<Int, Int>> = emptyList()
)