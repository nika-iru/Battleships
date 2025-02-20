package com.fuentes.battleships.models.game.data

data class Cell(
    val x: Int,
    val y: Int,
    var isShip: Boolean = false,
    var isHit: Boolean = false,
    var isMiss: Boolean = false
)
