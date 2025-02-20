package com.fuentes.battleships.models.data.Tile

import androidx.compose.ui.graphics.Color  // ✅ Import Color

enum class TileState(val color: Color) {
    UNKNOWN(Color.Blue),   // ✅ Default unknown tile color
    SHIP(Color.Green),     // ✅ Ship tile color
    HIT(Color.Red),        // ✅ Hit ship color
    MISS(Color.Gray);      // ✅ Missed shot color
}