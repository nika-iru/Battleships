package com.fuentes.battleships.models.data.logic

import com.fuentes.battleships.models.data.Ships.MainShip

class GameManager {
    val player1 = Player("Player 1")
    val player2 = Player("Player 2")

    fun placePlayerShip(ship: MainShip, startX: Int, startY: Int): Boolean {
        return player1.grid.placeShip(ship, startX, startY)
    }

    fun placeEnemyShip(ship: MainShip, startX: Int, startY: Int): Boolean {
        return player2.grid.placeShip(ship, startX, startY)
    }

}
