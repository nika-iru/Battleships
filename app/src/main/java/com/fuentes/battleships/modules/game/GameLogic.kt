package com.fuentes.battleships.modules.game

import android.util.Log
import com.fuentes.battleships.modules.game.multiplayer.data.GameSession
import com.fuentes.battleships.modules.game.singleplayer.data.Cell
import com.fuentes.battleships.modules.game.singleplayer.data.GameState
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlin.random.Random

class GameLogic {
    fun createInitialBoard(): List<Cell> {
        val board = mutableListOf<Cell>()
        for (y in 0..9) {
            for (x in 0..9) {
                board.add(Cell(x, y))
            }
        }
        return board
    }

    fun createBoardWithShips(
        ships: List<List<Pair<Int, Int>>>,
        opponentHits: List<Pair<Int, Int>>,
        opponentMisses: List<Pair<Int, Int>>
    ): List<Cell> {
        return createInitialBoard().map { cell ->
            val coordinate = Pair(cell.x, cell.y)
            val isShip = ships.any { ship ->
                ship.any { (shipX, shipY) -> shipX == cell.x && shipY == cell.y }
            }
            val isHit = isShip && coordinate in opponentHits
            val isMiss = coordinate in opponentMisses

            cell.copy(
                isShip = isShip,
                isHit = isHit,
                isMiss = isMiss
            )
        }
    }

    fun createBoardForAttacking(gameState: GameState): List<Cell> {
        val attackedShips = if (gameState.isPlayer1Turn) gameState.player2Ships else gameState.player1Ships
        val attackingHits = if (gameState.isPlayer1Turn) gameState.player1Hits else gameState.player2Hits
        val attackingMisses = if (gameState.isPlayer1Turn) gameState.player1Misses else gameState.player2Misses

        return createInitialBoard().map { cell ->
            val coordinate = Pair(cell.x, cell.y)
            val isHit = coordinate in attackingHits
            val isMiss = coordinate in attackingMisses

            // Only mark as ship if it was hit
            val isShip = isHit && attackedShips.any { ship -> coordinate in ship }

            cell.copy(
                // Never show enemy ships unless they were hit
                isShip = isShip,
                isHit = isHit,
                isMiss = isMiss
            )
        }
    }

    fun createBoardForAttackingMultiplayer(gameSession: GameSession): List<Cell> {
        val attackedShips = if (gameSession.isPlayer1Turn) gameSession.player2Ships else gameSession.player1Ships
        val attackingHits = if (gameSession.isPlayer1Turn) gameSession.player1Hits else gameSession.player2Hits
        val attackingMisses = if (gameSession.isPlayer1Turn) gameSession.player1Misses else gameSession.player2Misses

        return createInitialBoard().map { cell ->
            val coordinate = Pair(cell.x, cell.y)
            val isHit = coordinate in attackingHits
            val isMiss = coordinate in attackingMisses

            // Only mark as ship if it was hit
            val isShip = isHit && attackedShips.any { ship -> coordinate in ship }

            cell.copy(
                // Never show enemy ships unless they were hit
                isShip = isShip,
                isHit = isHit,
                isMiss = isMiss
            )
        }
    }

    fun calculateShipPositions(startX: Int, startY: Int, isHorizontal: Boolean): List<Pair<Int, Int>> {
        return if (isHorizontal) {
            listOf(
                Pair(startX, startY),
                Pair(startX + 1, startY),
                Pair(startX + 2, startY)
            )
        } else {
            listOf(
                Pair(startX, startY),
                Pair(startX, startY + 1),
                Pair(startX, startY + 2)
            )
        }
    }

    fun handleAttack(
        x: Int,
        y: Int,
        gameState: GameState,
        board: List<Cell>,
        onUpdate: (GameState, List<Cell>, Boolean) -> Unit
    ) {
        val attackedShips = if (gameState.isPlayer1Turn) gameState.player2Ships else gameState.player1Ships
        val attackingHits = if (gameState.isPlayer1Turn) gameState.player1Hits else gameState.player2Hits
        val attackingMisses = if (gameState.isPlayer1Turn) gameState.player1Misses else gameState.player2Misses

        val coordinate = Pair(x, y)

        // Check if cell was already attacked
        if (coordinate !in attackingHits && coordinate !in attackingMisses) {
            // Check if hit by looking through all ships
            val isHit = attackedShips.any { ship -> coordinate in ship }

            // Update hits or misses list
            val newAttackingHits = if (isHit) attackingHits + coordinate else attackingHits
            val newAttackingMisses = if (!isHit) attackingMisses + coordinate else attackingMisses

            // Update board and state
            val newBoard = board.map { cell ->
                if (cell.x == x && cell.y == y) {
                    cell.copy(
                        isHit = isHit,
                        isMiss = !isHit,
                        // Show ship if it was hit
                        isShip = isHit && attackedShips.any { ship ->
                            ship.any { (shipX, shipY) -> shipX == cell.x && shipY == cell.y }
                        }
                    )
                } else cell
            }

            // Update game state with new hits/misses
            var newState = if (gameState.isPlayer1Turn) {
                gameState.copy(player1Hits = newAttackingHits, player1Misses = newAttackingMisses)
            } else {
                gameState.copy(player2Hits = newAttackingHits, player2Misses = newAttackingMisses)
            }

            // Count hits to check for win condition
            // Each player has 2 ships × 3 cells = 6 cells total
            val totalShipCells = 6

            // Check for game over
            newState = if (newAttackingHits.size == totalShipCells) {
                newState.copy(phase = 2)
            } else {
                // Switch turns
                newState.copy(isPlayer1Turn = !newState.isPlayer1Turn, boardView = 0)
            }

            onUpdate(newState, newBoard, isHit)
        }
    }

    fun handleAttackMultiplayer(x: Int, y: Int, gameSession: GameSession, onUpdate: (GameSession) -> Unit) {
        val attackedShips = if (gameSession.isPlayer1Turn) gameSession.player2Ships else gameSession.player1Ships
        val attackingHits = if (gameSession.isPlayer1Turn) gameSession.player1Hits else gameSession.player2Hits
        val attackingMisses = if (gameSession.isPlayer1Turn) gameSession.player1Misses else gameSession.player2Misses

        val coordinate = Pair(x, y)

        if (coordinate !in attackingHits && coordinate !in attackingMisses) {
            val isHit = attackedShips.any { ship -> coordinate in ship }

            val newAttackingHits = if (isHit) attackingHits + coordinate else attackingHits
            val newAttackingMisses = if (!isHit) attackingMisses + coordinate else attackingMisses

            val updatedSession = gameSession.copy(
                player1Hits = if (gameSession.isPlayer1Turn) newAttackingHits else gameSession.player1Hits,
                player2Hits = if (!gameSession.isPlayer1Turn) newAttackingHits else gameSession.player2Hits,
                player1Misses = if (gameSession.isPlayer1Turn) newAttackingMisses else gameSession.player1Misses,
                player2Misses = if (!gameSession.isPlayer1Turn) newAttackingMisses else gameSession.player2Misses,
                isPlayer1Turn = !gameSession.isPlayer1Turn
            )

            // Update Firestore
            Firebase.firestore.collection("sessions").document(gameSession.sessionId!!)
                .update(updatedSession.toMap())
                .addOnSuccessListener {
                    onUpdate(updatedSession)
                }
                .addOnFailureListener { exception ->
                    Log.e("GameScreen", "Failed to update game session", exception)
                }
        }
    }

    fun handlePlacement(
        x: Int,
        y: Int,
        gameState: GameState,
        board: List<Cell>,
        onUpdate: (GameState, List<Cell>) -> Unit
    ) {
        val currentShips = if (gameState.isPlayer1Turn) gameState.player1Ships else gameState.player2Ships

        if (currentShips.size < 2) {
            val shipPositions = calculateShipPositions(x, y, gameState.isHorizontal)

            // Check if ship would be out of bounds
            if (shipPositions.all { (posX, posY) -> posX in 0..9 && posY in 0..9 }) {
                // Check if ship overlaps with existing ships
                val existingShipPositions = currentShips.flatten()
                if (shipPositions.none { it in existingShipPositions }) {
                    val newShips = currentShips + listOf(shipPositions)

                    // Update board to show ship
                    val newBoard = board.map { cell ->
                        if (shipPositions.any { (shipX, shipY) -> shipX == cell.x && shipY == cell.y }) {
                            cell.copy(isShip = true)
                        } else cell
                    }

                    var newState = if (gameState.isPlayer1Turn) {
                        gameState.copy(player1Ships = newShips)
                    } else {
                        gameState.copy(player2Ships = newShips)
                    }

                    // Switch turns or phases
                    if (newShips.size == 2) {
                        if (!gameState.isPlayer1Turn) {
                            // Both players have placed their ships
                            newState = newState.copy(
                                phase = 1,
                                isPlayer1Turn = true,
                                boardView = 0
                            )
                        } else {
                            // Switch to player 2's placement
                            newState = newState.copy(isPlayer1Turn = false)
                        }
                    }
                    onUpdate(newState, newBoard)
                }
            }
        }
    }

    fun handlePlacementMultiplayer(x: Int, y: Int, gameSession: GameSession, onUpdate: (GameSession) -> Unit) {
        val currentShips = if (gameSession.isPlayer1Turn) gameSession.player1Ships else gameSession.player2Ships

        if (currentShips.size < 2) {
            val shipPositions = calculateShipPositions(x, y, gameSession.isHorizontal)

            if (shipPositions.all { (posX, posY) -> posX in 0..9 && posY in 0..9 }) {
                val existingShipPositions = currentShips.flatten()
                if (shipPositions.none { it in existingShipPositions }) {
                    val newShips = currentShips + listOf(shipPositions)

                    val updatedSession = if (gameSession.isPlayer1Turn) {
                        gameSession.copy(player1Ships = newShips)
                    } else {
                        gameSession.copy(player2Ships = newShips)
                    }

                    // Update Firestore
                    Firebase.firestore.collection("sessions").document(gameSession.sessionId!!)
                        .update(updatedSession.toMap())
                        .addOnSuccessListener {
                            onUpdate(updatedSession)
                        }
                        .addOnFailureListener { exception ->
                            Log.e("GameScreen", "Failed to update game session", exception)
                        }
                }
            }
        }
    }

    //Computer Logic
    fun placeComputerShips(): List<List<Pair<Int, Int>>> {
        val newShips = mutableListOf<List<Pair<Int, Int>>>()

        while (newShips.size < 2) { // Place 2 ships
            val isHorizontal = Random.nextBoolean()
            val maxX = if (isHorizontal) 7 else 9 // Leave room for ship length
            val maxY = if (isHorizontal) 9 else 7

            val startX = Random.nextInt(0, maxX + 1)
            val startY = Random.nextInt(0, maxY + 1)

            val shipPositions = calculateShipPositions(startX, startY, isHorizontal)

            // Check for overlaps with existing ships
            val existingPositions = newShips.flatten()
            if (shipPositions.none { it in existingPositions }) {
                newShips.add(shipPositions)
            }
        }

        return newShips
    }

    // Get a list of adjacent coordinates (up, down, left, right)
    fun getAdjacentCoordinates(coordinate: Pair<Int, Int>): List<Pair<Int, Int>> {
        val (x, y) = coordinate
        return listOf(
            Pair(x - 1, y), // Left
            Pair(x + 1, y), // Right
            Pair(x, y - 1), // Up
            Pair(x, y + 1)  // Down
        )
    }

    // Get a random unattacked coordinate for computer targeting
    fun getRandomUnattackedCoordinate(hits: List<Pair<Int, Int>>, misses: List<Pair<Int, Int>>): Pair<Int, Int> {
        val attackedCoordinates = hits + misses
        var randomCoordinate: Pair<Int, Int>

        do {
            val x = Random.nextInt(0, 10)
            val y = Random.nextInt(0, 10)
            randomCoordinate = Pair(x, y)
        } while (randomCoordinate in attackedCoordinates)

        return randomCoordinate
    }
}