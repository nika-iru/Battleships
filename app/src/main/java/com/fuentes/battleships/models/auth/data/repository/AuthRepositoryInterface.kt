package com.fuentes.battleships.models.auth.data.repository

import com.google.firebase.auth.FirebaseUser

interface AuthRepositoryInterface {
    fun getCurrentUser(): FirebaseUser?
}