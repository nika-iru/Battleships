package com.fuentes.battleships.models.data

data class Cell(
    val x: Int,
    val y: Int,
    val isShip: Boolean = false,
    val isHit: Boolean = false,
    val isMiss: Boolean = false
)