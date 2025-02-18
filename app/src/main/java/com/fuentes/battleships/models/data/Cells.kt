package com.fuentes.battleships.models.data

data class Cell(
    val x: Int,
    val y: Int,
    var isShip: Boolean = false,
    var isHit: Boolean = false,
    var isMiss: Boolean = false
)
