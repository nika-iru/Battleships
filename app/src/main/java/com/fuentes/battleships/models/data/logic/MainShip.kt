package com.fuentes.battleships.models.data.logic

enum class Orientation { HORIZONTAL, VERTICAL }

// MainShip class
data class MainShip(val length: Int, val orientation: Orientation) {
    private var coords: MutableList<Pair<Int, Int>> = mutableListOf()  // Coordinates of the ship on the grid
    private var hits: Int = 0  // Track the number of hits on the ship

    // Attempt to place the ship on the grid at the given starting coordinates
    fun placeOnGrid(grid: Grid, startX: Int, startY: Int): Boolean {
        val newCoords = mutableListOf<Pair<Int, Int>>()

        // Check if all tiles the ship would occupy are free (i.e., they must be UNKNOWN)
        for (i in 0 until length) {
            val x = if (orientation == Orientation.HORIZONTAL) startX + i else startX
            val y = if (orientation == Orientation.VERTICAL) startY + i else startY
            val tile = grid.getTile(x, y)

            // If any of the tiles are already occupied (SHIP), the placement fails
            if (tile.state != TileState.UNKNOWN) {
                return false
            }
            newCoords.add(Pair(x, y))
        }

        // Place the ship on the grid (marking the tiles as SHIP)
        for (coord in newCoords) {
            val tile = grid.getTile(coord.first, coord.second)
            tile.placeShip(this)  // Link the ship to the tile and mark as SHIP
        }

        // Update the ship's coordinates
        coords = newCoords
        return true
    }

    // Check if the ship has been sunk (all its tiles are HIT)
    fun isSunk(opponentGrid: Grid): Boolean {
        return coords.all { opponentGrid.getTile(it.first, it.second).state == TileState.HIT }
    }


    // Mark the ship as hit on a specific tile
    fun markHit() {
        hits++
    }

    // Mark the ship as sunk (change all its tiles to SUNK)
    fun sinkShip(grid: Grid) {
        for (coord in coords) {
            val tile = grid.getTile(coord.first, coord.second)
            tile.markSunk()  // Mark the tile as SUNK
        }
    }
}
