package com.fuentes.battleships.modules.auth.data

data class User(
    val name: String,
    val id: String,
    val email: String,
    val wins: Int = 0
)