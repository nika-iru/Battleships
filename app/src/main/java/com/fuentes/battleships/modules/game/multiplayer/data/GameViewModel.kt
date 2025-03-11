package com.fuentes.battleships.modules.game.multiplayer.data

import androidx.lifecycle.ViewModel
import com.fuentes.battleships.modules.auth.ui.AuthState
import com.fuentes.battleships.modules.game.singleplayer.data.GameState
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class GameViewModel: ViewModel() {
    private val _gameState = MutableStateFlow(GameSession())
    val gameState: StateFlow<GameSession> = _gameState.asStateFlow()

    val db = Firebase.firestore
    val gameSessions = db.collection("sessions")

    suspend fun createGameSession(player1Id: String): GameSession? {
        // create game session
        val gameSession = GameSession(
            player1Id = player1Id
        )
        val docRef = gameSessions.add(gameSession).await()

        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            // convert and serialize to game session
            var gameSession = snapshot.toObject(GameSession::class.java)
            gameSession = gameSession?.copy(id = snapshot.id)

            return gameSession
        }

        return null
    }

    fun readGameSession(
        gameId: String,
        readOnce: Boolean,
        onRead: (GameSession?) -> Unit
    ) {
        val docRef = gameSessions.document(gameId)

        if (!readOnce) {
            docRef.addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    var gameSession = snapshot.toObject(GameSession::class.java)
                    gameSession = gameSession?.copy(id = snapshot.id)

                    onRead(gameSession)
                } else {
                    onRead(null)
                }
            }
            return
        }

        docRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                var gameSession = task.result.toObject(GameSession::class.java)
                gameSession = gameSession?.copy(id = task.result.id)

                onRead(gameSession)
            }
        }
    }

    fun updateGameSession(
        gameId: String,
        updated: GameSession
    ) {
        gameSessions.document(gameId).set(updated.toMap())
    }

    fun readOpenGameSessions(currentUserId: String, onRead: (List<GameSession>) -> Unit) {
        gameSessions.whereNotEqualTo("player1Id", currentUserId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val sessionDocs = snapshot.documents
                    var sessions = emptyList<GameSession>()

                    if (sessionDocs.isEmpty()) {
                        onRead(sessions)
                        return@addSnapshotListener
                    }

                    // only display game session that doesn't have any opponent
                    sessions = sessionDocs.filter { it.getString("player2Id") == null }
                        .map {
                            var gameSession = it.toObject(GameSession::class.java)
                            gameSession = gameSession?.copy(id = it.id)
                            gameSession as GameSession
                        }

                    onRead(sessions)
                }
            }
    }
}