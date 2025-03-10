package com.fuentes.battleships.modules.game.data.models

data class Cell(
    val x: Int,
    val y: Int,
    var isShip: Boolean = false,
    var isHit: Boolean = false,
    var isMiss: Boolean = false
)
