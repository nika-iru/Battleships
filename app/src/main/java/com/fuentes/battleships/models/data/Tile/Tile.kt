package com.fuentes.battleships.models.data.Tile

import com.fuentes.battleships.models.data.Ships.MainShip

data class Tile(val coord: Pair<Int, Int>, var state: TileState = TileState.UNKNOWN, var ship: MainShip? = null) {

    fun markHit(): Boolean {
        if (state == TileState.SHIP) {
            state = TileState.HIT
            ship?.let {
                if (it.registerHit()) {
                    return true  // ✅ Return true if the ship is eliminated
                }
            }
        }
        return false
    }

    fun markMiss() {
        if (state == TileState.UNKNOWN) {
            state = TileState.MISS
        }
    }

    fun hasShip(): Boolean = ship != null  // ✅ Fixed logic

    fun placeShip(ship: MainShip): Boolean {
        return if (state == TileState.UNKNOWN) {
            this.ship = ship
            state = TileState.SHIP
            true
        } else {
            false
        }
    }

    fun isAttacked(): Boolean = state == TileState.HIT || state == TileState.MISS
}