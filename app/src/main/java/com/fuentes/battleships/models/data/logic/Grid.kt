package com.fuentes.battleships.models.data.logic

class Grid(val name: String) {
    val tiles: MutableList<Tile> = mutableListOf()
    val ships: MutableList<MainShip> = mutableListOf()

    init {
        for (x in 0..9) {
            for (y in 0..9) {
                tiles.add(Tile(Pair(x, y)))
            }
        }
    }

    fun getTile(x: Int, y: Int): Tile {
        return tiles.find { it.coord == Pair(x, y) }
            ?: throw IllegalArgumentException("Invalid coordinates")
    }

    // Places a ship on the grid at the given coordinates if possible
    fun placeShip(ship: MainShip, startX: Int, startY: Int): Boolean {
        return if (ship.placeOnGrid(this, startX, startY)) {
            ships.add(ship)
            true
        } else {
            false
        }
    }

    // Check if all ships on the grid are sunk
    // Function to check if all ships on the grid are sunk
    fun allShipsSunk(): Boolean {
        return ships.all { it.isSunk(this) }  // Checks if all ships are sunk
    }


    fun attackTile(x: Int, y: Int): Boolean {
        val tile = getTile(x, y)

        if (tile.isAttacked()) return false // Don't attack the same tile again

        return if (tile.hasShip()) {
            tile.markHit() // Mark tile as hit
            val ship = tile.ship
            if (ship != null && ship.isSunk(this)) {
                ship.sinkShip(this)  // Mark all ship tiles as SUNK
                return true // This means a ship was fully sunk
            }
            true // A ship was hit but not necessarily sunk
        } else {
            tile.markMiss()
            false // Missed shot
        }
    }

}


