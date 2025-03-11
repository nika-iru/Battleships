/*
package com.fuentes.battleships.modules.game.multiplayer.data

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class FirestoreRepository: DataRepositoryInterface {
    private val db = Firebase.firestore

    override fun createGameSession(player1Id: String, onAdd: ((successful: Boolean) -> Unit)?) {
        val gameSession = GameSession(player1Id = player1Id)
        db.collection("gameSessions")
            .add(gameSession)
            .addOnSuccessListener {
                onAdd?.invoke(true)
            }
            .addOnFailureListener {
                onAdd?.invoke(false)
            }
    }

    override fun updateGameSession(sessionId: String) {
        db.collection("gameSessions")
    }
}*/
