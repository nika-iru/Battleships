package com.fuentes.battleships.modules.game.multiplayer.data

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuentes.battleships.modules.auth.ui.AuthViewModel
import com.fuentes.battleships.modules.game.GameLogic
import com.google.firebase.Firebase
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

    private var sessionListener: ListenerRegistration? = null

    private val db = Firebase.firestore
    private val gameLogic = GameLogic()

    private val _gameList = MutableStateFlow<List<GameList>>(emptyList())
        val gameList: StateFlow<List<GameList>> get() = _gameList



        // Create a new game session
    fun createGameSession(player1Id: String?, onAdd: ((successful: Boolean) -> Unit)?) {
        val gameSession = GameSession(
            sessionId = db.collection("sessions").document().id,
            player1Id = player1Id,
            status = "waiting"
        )
        db.collection("sessions")
            .document(gameSession.sessionId!!)
            .set(gameSession)
            .addOnSuccessListener {
                onAdd?.invoke(true)
            }
            .addOnFailureListener {
                onAdd?.invoke(false)
            }
    }

    fun joinGameSession(sessionId: String, opponentEmail: String) {
        fetchUserDetails(opponentEmail, { userId ->
            // Update the session in Firestore with the opponent's details
            db.collection("sessions").document(sessionId)
                .update(
                    "player2Id", userId,
                    "status", "playing" // Update status to "playing"
                )
                .addOnSuccessListener {
                    // Update the local game session state
                    _gameSession.update { currentSession ->
                        currentSession.copy(
                            sessionId = sessionId,
                            player2Id = userId,
                            status = "playing"
                        )
                    }

                    // Fetch the updated game session details
                    updateGameSession(sessionId)

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
    fun updateGameSession(sessionId: String) {
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
    }
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

    private fun fetchUserDetails(email: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("Users").whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.firstOrNull()?.let { document ->
                    onSuccess(document.getString("userId") ?: "")
                } ?: onFailure(Exception("User not found"))
            }
            .addOnFailureListener(onFailure)
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
    }

    // Handle ship placement
    fun handlePlacement(x: Int, y: Int, gameSession: GameSession, onUpdate: (GameSession) -> Unit) {
        if (gameSession.sessionId == null) {
            Log.e("GameViewModel", "Session ID is null. Cannot update Firestore.")
            return
        }

        val currentShips = if (gameSession.isPlayer1Turn) gameSession.player1Ships ?: emptyList() else gameSession.player2Ships ?: emptyList()

        if (currentShips.size < 2) {
            val shipPositions = gameLogic.calculateShipPositions(x, y, gameSession.isHorizontal)

            if (shipPositions.all { (posX, posY) -> posX in 0..9 && posY in 0..9 }) {
                val existingShipPositions = currentShips.flatten()
                if (shipPositions.none { it in existingShipPositions }) {
                    val newShips = currentShips + listOf(shipPositions)

                    val updatedSession = if (gameSession.isPlayer1Turn) {
                        gameSession.copy(player1Ships = newShips)
                    } else {
                        gameSession.copy(player2Ships = newShips)
                    }

                    db.collection("sessions").document(gameSession.sessionId!!)
                        .update(updatedSession.toMap())
                        .addOnSuccessListener {
                            onUpdate(updatedSession)
                        }
                        .addOnFailureListener { exception ->
                            Log.e("GameViewModel", "Failed to update game session", exception)
                        }
                }
            }
        }
    }

    // Handle attack
    fun handleAttack(x: Int, y: Int, gameSession: GameSession, onUpdate: (GameSession) -> Unit) {
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
            db.collection("sessions").document(gameSession.sessionId!!)
                .update(updatedSession.toMap())
                .addOnSuccessListener {
                    onUpdate(updatedSession)
                }
                .addOnFailureListener { exception ->
                    Log.e("GameViewModel", "Failed to update game session", exception)
                }
        }
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
        updateGameSession(sessionId)
    }
}


