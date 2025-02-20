package com.fuentes.battleships.models.data.Ships

import com.fuentes.battleships.models.data.Grid.Grid
import com.fuentes.battleships.models.data.Tile.TileState

enum class Orientation { HORIZONTAL, VERTICAL }

data class MainShip(val length: Int, val orientation: Orientation) {
    private var coordinates: MutableList<Pair<Int, Int>> = mutableListOf()
    private var hits: Int = 0  // Track number of hits

    fun placeOnGrid(grid: Grid, startX: Int, startY: Int): Boolean {
        val newCoords = mutableListOf<Pair<Int, Int>>()

        // Check if the ship can be placed within bounds and doesn't overlap
        for (i in 0 until length) {
            val x = if (orientation == Orientation.HORIZONTAL) startX + i else startX
            val y = if (orientation == Orientation.VERTICAL) startY + i else startY

            if (x !in 0..9 || y !in 0..9) return false  // Prevent out-of-bounds placement

            val tile = grid.getTile(x, y)
            if (tile.state != TileState.UNKNOWN) return false  // Prevent overlapping ships

            newCoords.add(Pair(x, y))
        }

        // If placement is valid, update the grid
        newCoords.forEach { (x, y) -> grid.getTile(x, y).placeShip(this) }

        coordinates = newCoords  // Save ship coordinates
        return true
    }

    fun registerHit(): Boolean {
        hits++
        return hits >= length  // ✅ Eliminated when all tiles are hit
    }
}
