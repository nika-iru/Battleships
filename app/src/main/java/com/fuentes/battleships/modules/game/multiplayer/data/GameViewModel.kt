package com.fuentes.battleships.modules.game.multiplayer.data

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuentes.battleships.modules.auth.ui.AuthViewModel
import com.fuentes.battleships.modules.game.singleplayer.data.GameLogic
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val _gameState = MutableStateFlow(GameSession())
    val gameState: StateFlow<GameSession> = _gameState

    private val _gameList = MutableStateFlow<List<GameList>>(emptyList())
    val gameList: StateFlow<List<GameList>> get() = _gameList

    private var sessionListener: ListenerRegistration? = null

    private val _gameSession = MutableStateFlow(GameSession())
    val gameSession: StateFlow<GameSession> = _gameSession.asStateFlow()

    private val authViewModel = AuthViewModel()

    private val db = Firebase.firestore
    private val gameLogic = GameLogic()

    fun createGameSession(player1Id: String?, onAdd: ((successful: Boolean) -> Unit)?) {
        val gameSession = GameSession(
            sessionId = db.collection("sessions").document().id,
            player1Id = authViewModel.getCurrentUserId(),
            status = "waiting"
        )
        db.collection("sessions")
            .add(gameSession)
            .addOnSuccessListener {
                onAdd?.invoke(true)
            }
            .addOnFailureListener {
                onAdd?.invoke(false)
            }
    }

    fun updateGameSession(sessionId: String) {
        _gameSession.update { it.copy(sessionId = sessionId) }

        val sessionId = _gameSession.value.sessionId ?: return

        sessionListener?.remove()

        sessionListener = db.collection("sessions")
            .document(sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Multiplayer", "Error fetching game session", error)
                    return@addSnapshotListener
                }
                snapshot?.let { doc ->

                    val player1Ships = (doc.get("player1Ships") as? List<List<List<Int>>>)
                        ?.map { ship -> ship.map { Pair(it[0], it[1]) } }
                        ?: emptyList()

                    val player2Ships = (doc.get("player2Ships") as? List<List<List<Int>>>)
                        ?.map { ship -> ship.map { Pair(it[0], it[1]) } }
                        ?: emptyList()

                    val player1Hits = (doc.get("player1Hits") as? List<List<Int>>)
                        ?.map { Pair(it[0], it[1]) }
                        ?: emptyList()

                    val player2Hits = (doc.get("player2Hits") as? List<List<Int>>)
                        ?.map { Pair(it[0], it[1]) }
                        ?: emptyList()

                    val player1Misses = (doc.get("player1Misses") as? List<List<Int>>)
                        ?.map { Pair(it[0], it[1]) }
                        ?: emptyList()

                    val player2Misses = (doc.get("player2Misses") as? List<List<Int>>)
                        ?.map { Pair(it[0], it[1]) }
                        ?: emptyList()

                    val isPlayer1Turn = doc.get("isPlayer1Turn") as? Boolean ?: true

                    val timer = doc.get("timer") as? Int ?: 15

                    _gameSession.update { currentSession ->
                        currentSession.copy(
                            sessionId = sessionId,
                            player1Ships = player1Ships,
                            player2Ships = player2Ships,
                            player1Hits = player1Hits,
                            player2Hits = player2Hits,
                            player1Misses = player1Misses,
                            player2Misses = player2Misses,
                            isPlayer1Turn = isPlayer1Turn,
                            timer = timer
                        )
                    }

                    //update local game state
                    _gameState.value = _gameState.value.copy(
                        sessionId = sessionId,
                        player1Ships = player1Ships,
                        player2Ships = player2Ships,
                        player1Hits = player1Hits,
                        player2Hits = player2Hits,
                        player1Misses = player1Misses,
                        player2Misses = player2Misses,
                        isPlayer1Turn = isPlayer1Turn,
                        timer = timer
                    )
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

}

