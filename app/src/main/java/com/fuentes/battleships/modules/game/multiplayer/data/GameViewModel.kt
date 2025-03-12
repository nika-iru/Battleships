package com.fuentes.battleships.modules.game.multiplayer.data

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuentes.battleships.modules.auth.ui.AuthViewModel
import com.fuentes.battleships.modules.game.GameLogic
import com.fuentes.battleships.modules.game.singleplayer.data.Cell
import com.fuentes.battleships.modules.game.singleplayer.data.GameState
import com.google.firebase.Firebase
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

                    snapshot?.let { doc ->
                        val updatedSession = GameSession(
                            sessionId = doc.id,
                            player1Id = doc.getString("player1Id"),
                            player2Id = doc.getString("player2Id"),
                            status = doc.getString("status") ?: "waiting",
                            player1Ships = (doc.get("player1Ships") as? List<List<List<Int>>>)?.map { ship ->
                                ship.map {
                                    Pair(
                                        it[0],
                                        it[1]
                                    )
                                }
                            } ?: emptyList(),
                            player2Ships = (doc.get("player2Ships") as? List<List<List<Int>>>)?.map { ship ->
                                ship.map {
                                    Pair(
                                        it[0],
                                        it[1]
                                    )
                                }
                            } ?: emptyList(),
                            player1Hits = (doc.get("player1Hits") as? List<List<Int>>)?.map {
                                Pair(
                                    it[0],
                                    it[1]
                                )
                            } ?: emptyList(),
                            player2Hits = (doc.get("player2Hits") as? List<List<Int>>)?.map {
                                Pair(
                                    it[0],
                                    it[1]
                                )
                            } ?: emptyList(),
                            player1Misses = (doc.get("player1Misses") as? List<List<Int>>)?.map {
                                Pair(
                                    it[0],
                                    it[1]
                                )
                            } ?: emptyList(),
                            player2Misses = (doc.get("player2Misses") as? List<List<Int>>)?.map {
                                Pair(
                                    it[0],
                                    it[1]
                                )
                            } ?: emptyList(),
                            isPlayer1Turn = doc.getBoolean("isPlayer1Turn") ?: true,
                            phase = doc.getLong("phase")?.toInt() ?: 0,
                            timer = doc.getLong("timer")?.toInt() ?: 15,
                            winnerId = doc.getString("winnerId")
                        )
                        _gameSession.value = updatedSession
                        Log.d(
                            "GameViewModel",
                            "Listening for changes on session: ${gameSession.value.sessionId}"
                        )
                        Log.d("GameViewModel", "Game Session Values: ${gameSession.value}")
                    }
                } else {
                    Log.e("GameViewModel", "Session document does not exist: $sessionId")
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
        Log.d("GameViewModel", "Initializing game with sessionId: $sessionId")
        _gameSession.update { it.copy(sessionId = sessionId) } // Add this line to set sessionId immediately
        listenForSessionUpdates(sessionId)
    }

    fun flattenShipCoordinates(ships: List<List<Pair<Int, Int>>>): List<String> {
        val flatList = mutableListOf<String>()

        for (ship in ships) {
            flatList.add("START_SHIP")
            for (coordinate in ship) {
                flatList.add("${coordinate.first},${coordinate.second}")
            }
            flatList.add("END_SHIP")
        }

        return flatList
    }

    /**
     * Reconstructs the nested ship coordinates from the flattened Firestore representation
     */
    fun reconstructShipCoordinatesFromFirestore(flatData: List<String>): List<List<Pair<Int, Int>>> {
        val ships = mutableListOf<List<Pair<Int, Int>>>()
        var currentShip = mutableListOf<Pair<Int, Int>>()

        for (item in flatData) {
            when (item) {
                "START_SHIP" -> {
                    // Start a new ship
                    currentShip = mutableListOf()
                }
                "END_SHIP" -> {
                    // End current ship and add to ships list
                    ships.add(currentShip)
                }
                else -> {
                    // Parse coordinate string back to Pair<Int, Int>
                    val parts = item.split(",")
                    if (parts.size == 2) {
                        try {
                            val x = parts[0].toInt()
                            val y = parts[1].toInt()
                            currentShip.add(Pair(x, y))
                        } catch (e: NumberFormatException) {
                            // Handle parsing error if needed
                        }
                    }
                }
            }
        }

        return ships
    }
}


