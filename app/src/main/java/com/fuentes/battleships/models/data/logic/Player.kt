package com.fuentes.battleships.models.data.logic

import com.fuentes.battleships.models.data.Grid.Grid
import com.fuentes.battleships.models.data.Ships.MainShip

class Player(val name: String) {
    val grid = Grid(name)
    private val ships = mutableListOf<MainShip>()

    fun placeShip(ship: MainShip, startX: Int, startY: Int): Boolean {
        return if (ship.placeOnGrid(grid, startX, startY)) {
            ships.add(ship)
            true
        } else {
            false
        }
    }

    fun attack(opponent: Player, x: Int, y: Int): Boolean {
        return opponent.grid.attackTile(x, y)
    }
}