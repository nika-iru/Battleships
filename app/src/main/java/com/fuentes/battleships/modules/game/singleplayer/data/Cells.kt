package com.fuentes.battleships.modules.game.singleplayer.data

data class Cell(
    val index: Int,
    var isShip: Boolean = false,
    var isHit: Boolean = false,
    var isMiss: Boolean = false
)