/* package com.fuentes.battleships.modules.game.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuentes.battleships.modules.auth.ui.AuthViewModel
import com.fuentes.battleships.modules.game.data.models.GameSession
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GameViewModel(private val authViewModel: AuthViewModel) : ViewModel() {

    private val db = Firebase.firestore
    private val gameSessions = db.collection("games")

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var gameSessionListener: ListenerRegistration? = null

    fun createGameSession() {
        viewModelScope.launch {
            //val user = currentUser ?: return@launch
            val user =
                authViewModel.uiState.value.userId ?: return@launch // Get userId from AuthViewModel
            val gameSession = GameSession(
                player1Id = user,
                // Initialize other fields as needed
            )
            val docRef = gameSessions.add(gameSession.toMap()).await()
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val createdSession = snapshot.toObject(GameSession::class.java)
                _gameState.update { it.copy(gameSession = createdSession?.copy(id = snapshot.id)) }
                // Start listening for updates immediately after creation
                subscribeToGameSession(createdSession?.id)
            }
        }
    }

    fun joinGameSession(gameId: String) {
        viewModelScope.launch {
            //val user = currentUser ?: return@launch
            val user =
                authViewModel.uiState.value.userId ?: return@launch // Get userId from AuthViewModel
            val gameSession = getGameSession(gameId)
            if (gameSession != null) {
                val updatedSession = gameSession.copy(player2Id = user)
                updateGameSession(updatedSession)
                // Start listening for updates after joining
                subscribeToGameSession(gameId)
            }
        }
    }

    private suspend fun getGameSession(gameId: String): GameSession? {
        val snapshot = gameSessions.document(gameId).get().await()
        return snapshot.toObject(GameSession::class.java)?.copy(id = snapshot.id)
    }

    private fun updateGameSession(gameSession: GameSession) {
        gameSession.id?.let { id ->
            gameSessions.document(id).set(gameSession.toMap())
        }
    }

    fun subscribeToGameSession(gameId: String?) {
        gameId?.let { id ->
            gameSessionListener = gameSessions.document(id)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Handle error
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val updatedSession =
                            snapshot.toObject(GameSession::class.java)?.copy(id = snapshot.id)
                        _gameState.update { it.copy(gameSession = updatedSession) }
                        updateLocalBoard() // Update the local board based on new data
                    }
                }
        }
    }

    fun unsubscribeFromGameSession() {
        gameSessionListener?.remove()
    }

    //Game Actions

    fun placeShip(x: Int, y: Int) {
        viewModelScope.launch {
            //val user = currentUser ?: return@launch
            val user =
                authViewModel.uiState.value.userId ?: return@launch // Get userId from AuthViewModel
            val session = _gameState.value.gameSession ?: return@launch
            val playerId = user
            val ships =
                if (playerId == session.player1Id) session.player1Ships else session.player2Ships
            val updatedShips = ships + listOf(calculateShipPositions(x, y, session.isHorizontal))
            val updatedSession = if (playerId == session.player1Id) {
                session.copy(player1Ships = updatedShips)
            } else {
                session.copy(player2Ships = updatedShips)
            }
            updateGameSession(updatedSession)
        }
    }

    fun attackCell(x: Int, y: Int) {
        viewModelScope.launch {
            //val user = currentUser ?: return@launch
            val user =
                authViewModel.uiState.value.userId ?: return@launch // Get userId from AuthViewModel
            val session = _gameState.value.gameSession ?: return@launch
            val playerId = user

            val isPlayer1 = playerId == session.player1Id
            val ships = if (isPlayer1) session.player2Ships else session.player1Ships
            val hits = if (isPlayer1) session.player1Hits else session.player2Hits
            val misses = if (isPlayer1) session.player1Misses else session.player2Misses

            val coordinate = Pair(x, y)
            val isHit = ships.any { ship -> coordinate in ship }
            val updatedHits = if (isHit) hits + coordinate else hits
            val updatedMisses = if (!isHit) misses + coordinate else misses

            val updatedSession = if (isPlayer1) {
                session.copy(
                    player1Hits = updatedHits,
                    player1Misses = updatedMisses
                )
            } else {
                session.copy(
                    player2Hits = updatedHits,
                    player2Misses = updatedMisses
                )
            }
            updateGameSession(updatedSession)
        }
    }

    //UI Logic

    private fun updateLocalBoard() {
        val session = _gameState.value.gameSession ?: return
        //val user = currentUser ?: return
        val user = authViewModel.uiState.value.userId ?: return // Get userId from AuthViewModel

        val playerId = user
        val isPlayer1 = playerId == session.player1Id
        val ships = if (isPlayer1) session.player1Ships else session.player2Ships
        val opponentHits = if (isPlayer1) session.player2Hits else session.player1Hits
        val opponentMisses = if (isPlayer1) session.player2Misses else session.player1Misses

        val newBoard = createInitialBoard().map { cell ->
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
        _gameState.update { it.copy(localBoard = newBoard) }
    }

    override fun onCleared() {
        super.onCleared()
        unsubscribeFromGameSession() // Stop listening when ViewModel is destroyed
    }
}
*/