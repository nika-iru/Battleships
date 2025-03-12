package com.fuentes.battleships.modules.game

import android.util.Log
import com.fuentes.battleships.modules.game.multiplayer.data.GameSession
import com.fuentes.battleships.modules.game.singleplayer.data.Cell
import com.fuentes.battleships.modules.game.singleplayer.data.GameState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlin.random.Random

class GameLogic {
    var db = Firebase.firestore
    var currentUser = Firebase.auth.currentUser?.uid
    val userDocRef = db.collection("Users").document(currentUser.toString())

    fun createInitialBoard(): List<Cell> {
        val board = List(100) { index -> Cell(index) }
        return board
    }


    fun createBoardWithShips(
        ships: List<List<Int>>,
        opponentHits: List<Int>,
        opponentMisses: List<Int>
    ): List<Cell> {
        return createInitialBoard().map { cell ->
            val coordinate = cell.index
            val isShip = ships.any { ship ->
                ship.any { shipIndex -> shipIndex == cell.index }
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
        val attackedShips =
            if (gameState.isPlayer1Turn) gameState.player2Ships else gameState.player1Ships
        val attackingHits =
            if (gameState.isPlayer1Turn) gameState.player1Hits else gameState.player2Hits
        val attackingMisses =
            if (gameState.isPlayer1Turn) gameState.player1Misses else gameState.player2Misses

        return createInitialBoard().map { cell ->
            val coordinate = cell.index
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

    /*fun createBoardForAttackingMultiplayer(gameSession: GameSession): List<Cell> {
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
    }*/

    fun calculateShipPositions(startIndex: Int, isHorizontal: Boolean): List<Int> {
        return if (isHorizontal) {
            listOf(
                startIndex,
                startIndex + 1,
                startIndex + 2
            )
        } else {
            listOf(
                startIndex,
                startIndex + 10,
                startIndex + 20
            )
        }
    }

    fun handleAttack(
        index: Int,
        gameState: GameState,
        board: List<Cell>,
        onUpdate: (GameState, List<Cell>, Boolean) -> Unit
    ) {
        val attackedShips =
            if (gameState.isPlayer1Turn) gameState.player2Ships else gameState.player1Ships
        val attackingHits =
            if (gameState.isPlayer1Turn) gameState.player1Hits else gameState.player2Hits
        val attackingMisses =
            if (gameState.isPlayer1Turn) gameState.player1Misses else gameState.player2Misses

        // Check if cell was already attacked
        if (index !in attackingHits && index !in attackingMisses) {
            // Check if hit by looking through all ships
            val isHit = attackedShips.any { ship -> index in ship }

            // Update hits or misses list
            val newAttackingHits = if (isHit) attackingHits + index else attackingHits
            val newAttackingMisses = if (!isHit) attackingMisses + index else attackingMisses

            // Update board and state
            val newBoard = board.map { cell ->
                if (cell.index == index) {
                    cell.copy(
                        isHit = isHit,
                        isMiss = !isHit,
                        // Show ship if it was hit
                        isShip = isHit && attackedShips.any { ship ->
                            ship.any { shipIndex -> shipIndex == cell.index }
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
                if (gameState.isPlayer1Turn) {
                    userDocRef.get().addOnSuccessListener { document ->
                        if (document.exists()) {
                            val currentWins = document.getLong("wins") ?: 0
                            userDocRef.update("wins", currentWins + 1)
                                .addOnSuccessListener {
                                    Log.d("Firestore", "Wins updated successfully")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Error updating wins", e)
                                }
                        } else {
                            // If user document doesn't exist, create it with wins = 1
                            userDocRef.set(mapOf("wins" to 1))
                                .addOnSuccessListener {
                                    Log.d("Firestore", "User document created with wins = 1")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Error creating user document", e)
                                }
                        }
                    }.addOnFailureListener { e ->
                        Log.e("Firestore", "Error retrieving user document", e)
                    }
                }
                newState.copy(phase = 2)
            } else {
                // Switch turns
                newState.copy(isPlayer1Turn = !newState.isPlayer1Turn, boardView = 0)

            }

            onUpdate(newState, newBoard, isHit)
        }
    }

    fun handlePlacement(
        index: Int,
        gameState: GameState,
        board: List<Cell>,
        onUpdate: (GameState, List<Cell>) -> Unit
    ) {
        val currentShips =
            if (gameState.isPlayer1Turn) gameState.player1Ships else gameState.player2Ships

        if (currentShips.size < 2) {
            val shipPositions = calculateShipPositions(index, gameState.isHorizontal)

            // Check if ship would be out of bounds
            if (checkIfOutOfBounds(index)) {
                // Check if ship overlaps with existing ships
                val existingShipPositions = currentShips.flatten()
                if (shipPositions.none { it in existingShipPositions }) {
                    val newShips = currentShips + listOf(shipPositions)

                    // Update board to show ship
                    val newBoard = board.map { cell ->
                        if (shipPositions.any { shipIndex -> shipIndex == cell.index }) {
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

    /*fun handlePlacementMultiplayer(x: Int, y: Int, gameSession: GameSession, onUpdate: (GameSession) -> Unit) {
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
    }*/

    //Computer Logic
    fun placeComputerShips(): List<List<Int>> {
        val newShips = mutableListOf<List<Int>>()

        while (newShips.size < 2) { // Place 2 ships
            val isHorizontal = Random.nextBoolean()

            val x = Random.nextInt(0, 7)
            val y = Random.nextInt(0, 7)
            val index = x + y * 10

            val shipPositions = calculateShipPositions(index, isHorizontal)

            // Check for overlaps with existing ships
            val existingPositions = newShips.flatten()
            if (shipPositions.none { it in existingPositions }) {
                newShips.add(shipPositions)
            }
        }

        return newShips
    }

    // Get a list of adjacent coordinates (up, down, left, right)
    fun getAdjacentCoordinates(coordinate: Int): List<Int> {
        val index = coordinate
        return listOf(
            index - 1, // Left
            index + 1, // Right
            index - 10, // Up
            index + 10  // Down
        )
    }

    // Get a random unattacked coordinate for computer targeting
    fun getRandomUnattackedCoordinate(hits: List<Int>, misses: List<Int>): Int {
        val attackedCoordinates = hits + misses
        var randomCoordinate: Int

        do {
            val index = Random.nextInt(0, 99)
            randomCoordinate = index
        } while (randomCoordinate in attackedCoordinates)

        return randomCoordinate
    }

    fun checkIfOutOfBounds(index: Int): Boolean {
        if (
            index in 0..7 || index in 10..17 || index in 20..27 || index in 30..37 || index in 40..47 || index in 50..57 || index in 60..67 || index in 70..77) {
            return true
        }
        return false
    }
}