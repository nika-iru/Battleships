package com.fuentes.battleships.models.data.Grid

import com.fuentes.battleships.models.data.Tile.Tile
import com.fuentes.battleships.models.data.Ships.MainShip
import com.fuentes.battleships.models.data.Tile.TileState

class Grid(val name: String) { // ✅ Added a name parameter
    val tiles: MutableList<Tile> = mutableListOf()
    private val ships: MutableList<MainShip> = mutableListOf()
    val eliminatedShips: MutableList<MainShip> = mutableListOf()

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

    fun placeShip(ship: MainShip, startX: Int, startY: Int): Boolean {
        return if (ship.placeOnGrid(this, startX, startY)) {
            ships.add(ship)
            true
        } else {
            false
        }
    }

    fun attackTile(x: Int, y: Int): Boolean {
        val tile = getTile(x, y)
        if (tile.isAttacked()) return false // Prevent re-attacking the same tile

        return if (tile.hasShip()) {
            val ship = tile.ship

            val isEliminated = tile.markHit() // ✅ Now returns true if ship is eliminated
            if (isEliminated && ship != null && !eliminatedShips.contains(ship)) {
                eliminatedShips.add(ship)  // ✅ Mark ship as eliminated
                ships.remove(ship)  // ✅ Remove from active fleet
            }

            true // Attack was a hit
        } else {
            tile.markMiss()
            false // Attack was a miss
        }
    }

    fun allShipsEliminated(): Boolean {
        return eliminatedShips.size == 3  // ✅ Ensure all ships are accounted for
    }
}
