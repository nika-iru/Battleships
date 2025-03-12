package com.fuentes.battleships.modules.game.multiplayer.data

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuentes.battleships.modules.auth.ui.AuthViewModel
import com.fuentes.battleships.modules.game.GameLogic
import com.fuentes.battleships.modules.game.singleplayer.data.Cell
import com.fuentes.battleships.modules.game.singleplayer.data.GameState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val _gameSession = MutableStateFlow(GameSession())
    val gameSession: StateFlow<GameSession> = _gameSession.asStateFlow()

    private val authViewModel = AuthViewModel()
    private var sessionListener: ListenerRegistration? = null

    private val db = Firebase.firestore
    private val gameLogic = GameLogic()

    private val _gameList = MutableStateFlow<List<GameList>>(emptyList())
    val gameList: StateFlow<List<GameList>> get() = _gameList


    // Create a new game session
    fun createGameSession(
        player1Id: String?,
        onAdd: ((successful: Boolean, sessionId: String?) -> Unit)?
    ) {
        val gameSession = GameSession(
            sessionId = db.collection("sessions").document().id,
            player1Id = player1Id,
            status = "waiting"
        )
        db.collection("sessions")
            .document(gameSession.sessionId!!)
            .set(gameSession)
            .addOnSuccessListener {
                listenForSessionUpdates(gameSession.sessionId)
                onAdd?.invoke(true, gameSession.sessionId)  // Pass both success state and sessionId
            }
            .addOnFailureListener {
                onAdd?.invoke(false, null)
            }
    }

    fun joinGameSession(sessionId: String, opponentEmail: String) {
        fetchUserDetails(opponentEmail, { userId ->
            // Update the session in Firestore with the opponent's details
            db.collection("sessions").document(sessionId)
                .update(
                    "player2Id", authViewModel.getCurrentUserId(),
                    "status", "playing" // Update status to "playing"
                )
                .addOnSuccessListener {
                    Log.d("Join Game Session", "Joining game session: $sessionId, with userId: $userId")
                    // Update the local game session state
                    _gameSession.update { currentSession ->
                        currentSession.copy(
                            sessionId = sessionId,
                            player2Id = userId,
                            status = "playing"
                        )
                    }

                    // Fetch the updated game session details
                    listenForSessionUpdates(sessionId)

                    Log.d("Multiplayer", "Successfully joined game session: $sessionId")
                }
                .addOnFailureListener { exception ->
                    Log.e("Multiplayer", "Failed to join game session", exception)
                }
        }, { exception ->
            Log.e("Multiplayer", "Failed to fetch opponent details", exception)
        })
    }

    // Update the local game session state from Firestore
    /*fun updateGameSession(sessionId: String) {
        sessionListener?.remove()

        sessionListener = db.collection("sessions")
            .document(sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GameViewModel", "Error fetching game session", error)
                    return@addSnapshotListener
                }

                snapshot?.let { doc ->
                    val updatedSession = GameSession(
                        sessionId = doc.id,
                        player1Id = doc.getString("player1Id"),
                        player2Id = doc.getString("player2Id"),
                        status = doc.getString("status") ?: "waiting",
                        player1Ships = (doc.get("player1Ships") as? List<List<List<Int>>>)?.map { ship -> ship.map { Pair(it[0], it[1]) } } ?: emptyList(),
                        player2Ships = (doc.get("player2Ships") as? List<List<List<Int>>>)?.map { ship -> ship.map { Pair(it[0], it[1]) } } ?: emptyList(),
                        player1Hits = (doc.get("player1Hits") as? List<List<Int>>)?.map { Pair(it[0], it[1]) } ?: emptyList(),
                        player2Hits = (doc.get("player2Hits") as? List<List<Int>>)?.map { Pair(it[0], it[1]) } ?: emptyList(),
                        player1Misses = (doc.get("player1Misses") as? List<List<Int>>)?.map { Pair(it[0], it[1]) } ?: emptyList(),
                        player2Misses = (doc.get("player2Misses") as? List<List<Int>>)?.map { Pair(it[0], it[1]) } ?: emptyList(),
                        isPlayer1Turn = doc.getBoolean("isPlayer1Turn") ?: true,
                        phase = doc.getLong("phase")?.toInt() ?: 0,
                        timer = doc.getLong("timer")?.toInt() ?: 15,
                        winnerId = doc.getString("winnerId")
                    )
                    _gameSession.value = updatedSession
                }
            }
    }*/

    fun fetchGameSessions() {
        db.collection("sessions")
            .whereEqualTo("status", "waiting") // Only fetch waiting sessions
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Multiplayer", "Error fetching game sessions", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    viewModelScope.launch {
                        _gameList.value = snapshot.documents.mapNotNull { doc ->
                            GameList(
                                player1Id = doc.getString("player1Id") ?: "Unknown",
                                sessionId = doc.id
                            )
                        }
                    }
                }
            }
    }

    private fun fetchUserDetails(
        email: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("Users")
            .whereEqualTo("email", email) // Query by email
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    // Get the first document (assuming email is unique)
                    val document = snapshot.documents[0]
                    val userId = document.getString("id") // Ensure "userId" is the correct field name
                    if (userId != null) {
                        onSuccess(userId)
                    } else {
                        onFailure(Exception("userId field is missing in Firestore"))
                    }
                } else {
                    onFailure(Exception("User not found in Firestore"))
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun updateGameSession(updatedSession: GameSession) {
        val sessionId = updatedSession.sessionId
        if (sessionId != null) {
            db.collection("sessions").document(sessionId)

                .update(updatedSession.toMap())
                .addOnSuccessListener {
                    Log.d("GameViewModel", "Game session updated successfully")
                }
                .addOnFailureListener { exception ->
                    Log.e("GameViewModel", "Failed to update game session", exception)
                }
        }
    }

    fun listenForSessionUpdates(sessionId: String) {
        if (sessionId.isEmpty()) {
            Log.e("GameViewModel", "Cannot listen for updates: sessionId is empty")
            return
        }

        sessionListener?.remove()

        Log.d("GameViewModel", "Setting up listener for session: $sessionId")
        sessionListener = db.collection("sessions")
            .document(sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GameViewModel", "Error fetching game session", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    snapshot.let { doc ->
                        val player1Ships = unflattenShips((doc.get("player1Ships") as? List<Long>)?.map { it.toInt() } ?: emptyList())
                        val player2Ships = unflattenShips((doc.get("player2Ships") as? List<Long>)?.map { it.toInt() } ?: emptyList())
                        val updatedSession = GameSession(
                            sessionId = doc.id,
                            player1Id = doc.getString("player1Id"),
                            player2Id = doc.getString("player2Id"),
                            status = doc.getString("status") ?: "waiting",
                            player1Ships = player1Ships,
                            player2Ships = player2Ships,
                            player1Hits = (doc.get("player1Hits") as? List<Long>)?.map { it.toInt() } ?: emptyList(),
                            player2Hits = (doc.get("player2Hits") as? List<Long>)?.map { it.toInt() } ?: emptyList(),
                            player1Misses = (doc.get("player1Misses") as? List<Long>)?.map { it.toInt() } ?: emptyList(),
                            player2Misses = (doc.get("player2Misses") as? List<Long>)?.map { it.toInt() } ?: emptyList(),
                            isPlayer1Turn = doc.getBoolean("isPlayer1Turn") ?: true,
                            phase = doc.getLong("phase")?.toInt() ?: 0,
                            timer = doc.getLong("timer")?.toInt() ?: 15,
                            winnerId = doc.getString("winnerId"),
                            showTurnNotification = doc.getBoolean("showTurnNotification") ?: false,
                            turnMessage = doc.getString("turnMessage") ?: ""
                        )
                        _gameSession.value = updatedSession
                    }
                } else {
                    Log.e("GameViewModel", "Session document does not exist: $sessionId")
                }
            }
    }

    private fun unflattenShips(flattenedShips: List<Int>): List<List<Int>> {
        return flattenedShips.chunked(3) // Assuming each ship has 3 positions
    }

    fun dismissTurnNotification(gameSession: GameSession) {
        updateGameSession(gameSession.copy(showTurnNotification = false))
    }

    // Reset the game session
    fun resetGameSession(sessionId: String) {
        db.collection("sessions").document(sessionId)
            .delete()
            .addOnSuccessListener {
                _gameSession.value = GameSession()
            }
            .addOnFailureListener { exception ->
                Log.e("GameViewModel", "Failed to reset game session", exception)
            }
    }

    fun initializeGame(sessionId: String) {
        Log.d("GameViewModel", "Initializing game with sessionId: $sessionId")
        _gameSession.update { it.copy(sessionId = sessionId) } // Add this line to set sessionId immediately
        listenForSessionUpdates(sessionId)
    }

    fun handleTimerExpired(gameSession: GameSession) {
        val updatedSession = gameSession.copy(
            isPlayer1Turn = !gameSession.isPlayer1Turn,
            timer = 15, // Reset the timer
            showTurnNotification = true,
            turnMessage = "Time's up! Player ${if (!gameSession.isPlayer1Turn) 1 else 2}'s turn"
        )

        updateGameSession(updatedSession)
    }


    fun handlePlacement(index: Int, gameSession: GameSession, gameViewModel: GameViewModel) {
        val currentShips =
            if (gameSession.isPlayer1Turn) gameSession.player1Ships else gameSession.player2Ships

        if (currentShips.size < 2) {
            val shipPositions = calculateShipPositions(index, gameSession.isHorizontal)

            if (checkIfOutOfBounds(index, gameSession)) {
                val existingShipPositions = currentShips.flatten()
                if (shipPositions.none { it in existingShipPositions }) {
                    val newShips = currentShips + listOf(shipPositions)

                    val updatedSession = if (gameSession.isPlayer1Turn) {
                        gameSession.copy(player1Ships = newShips)
                    } else {
                        gameSession.copy(player2Ships = newShips)
                    }

                    if (newShips.size == 2) {
                        if (!gameSession.isPlayer1Turn) {
                            // Both players have placed their ships, transition to battle phase
                            gameViewModel.updateGameSession(
                                updatedSession.copy(
                                    phase = 1, // Battle phase
                                    isPlayer1Turn = true // Player 1 starts the battle
                                )
                            )
                        } else {
                            // Player 1 has placed their ships, switch to Player 2's turn for placement
                            gameViewModel.updateGameSession(
                                updatedSession.copy(
                                    isPlayer1Turn = false // Switch to Player 2's turn
                                )
                            )
                        }
                    } else {
                        // Update the session without changing the phase or turn
                        gameViewModel.updateGameSession(updatedSession)
                    }
                }
            }
        }
    }

    fun handleAttack(
        index: Int,
        gameSession: GameSession,
        gameViewModel: GameViewModel,
        onResult: (Boolean) -> Unit
    ) {
        val attackedShips =
            if (gameSession.isPlayer1Turn) gameSession.player2Ships else gameSession.player1Ships
        val attackingHits =
            if (gameSession.isPlayer1Turn) gameSession.player1Hits else gameSession.player2Hits
        val attackingMisses =
            if (gameSession.isPlayer1Turn) gameSession.player1Misses else gameSession.player2Misses

        if (index !in attackingHits && index !in attackingMisses) {
            val isHit = attackedShips.any { ship -> index in ship }

            val newAttackingHits = if (isHit) attackingHits + index else attackingHits
            val newAttackingMisses = if (!isHit) attackingMisses + index else attackingMisses

            val updatedSession = if (gameSession.isPlayer1Turn) {
                gameSession.copy(player1Hits = newAttackingHits, player1Misses = newAttackingMisses)
            } else {
                gameSession.copy(player2Hits = newAttackingHits, player2Misses = newAttackingMisses)
            }

            if (newAttackingHits.size == 6) {
                gameViewModel.updateGameSession(
                    updatedSession.copy(
                        phase = 2,
                        winnerId = if (gameSession.isPlayer1Turn) gameSession.player1Id else gameSession.player2Id
                    )
                )
            } else {
                gameViewModel.updateGameSession(updatedSession.copy(isPlayer1Turn = !gameSession.isPlayer1Turn, timer = 15))
            }

            onResult(isHit)
        }
    }

    fun calculateShipPositions(startIndex: Int, isHorizontal: Boolean): List<Int> {
        return if (isHorizontal) {
            listOf(startIndex, startIndex + 1, startIndex + 2)
        } else {
            listOf(startIndex, startIndex + 10, startIndex + 20)
        }
    }

    fun checkIfOutOfBounds(index: Int, gameSession: GameSession): Boolean {
        val row = index / 10
        val col = index % 10

        return if (gameSession.isHorizontal) {
            col <= 7  // Ensure there's room for 3 cells (col + 0, col + 1, col + 2)
        } else {
            row <= 7  // Ensure there's room for 3 cells (row + 0, row + 1, row + 2)
        }
    }
}


