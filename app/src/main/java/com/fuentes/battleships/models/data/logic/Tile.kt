package com.fuentes.battleships.models.data.logic

// Possible states for each tile
enum class TileState { UNKNOWN, MISS, HIT, SHIP, SUNK }

// Represents a single tile in the grid
data class Tile(val coord: Pair<Int, Int>, var state: TileState = TileState.UNKNOWN, var ship: MainShip? = null) {

    // Marks the tile as HIT if it contains a ship and updates the ship's hit status
    fun markHit() {
        if (state == TileState.SHIP) {
            state = TileState.HIT
            ship?.markHit()  // Mark the ship as hit
        }
    }

    // Marks the tile as MISS if it hasn't been attacked yet
    fun markMiss() {
        if (state == TileState.UNKNOWN) {
            state = TileState.MISS
        }
    }

    // Marks the tile as SUNK if it has been part of a fully sunk ship
    fun markSunk() {
        if (state == TileState.HIT) {
            state = TileState.SUNK  // Change state to SUNK when the ship is fully hit
        }
    }

    // Checks if the tile contains a ship
    fun hasShip(): Boolean = state == TileState.SHIP

    // Places a ship on this tile (only if it hasn't been attacked)
    fun placeShip(ship: MainShip): Boolean {
        return if (state == TileState.UNKNOWN) {
            this.ship = ship  // Link the ship to the tile
            state = TileState.SHIP
            true
        } else {
            false // Ship placement failed (already occupied or attacked)
        }
    }

    // Checks if the tile has already been attacked
    fun isAttacked(): Boolean = state == TileState.HIT || state == TileState.MISS
}