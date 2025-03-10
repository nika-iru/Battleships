package com.fuentes.battleships.models.auth.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class FirebaseAuthRepository: AuthRepositoryInterface {
    private val auth: FirebaseAuth = Firebase.auth

    override fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
}