package com.fuentes.battleships.modules.auth.ui

data class User(
    val name: String,
    val id: String,
    val email: String,
    val wins: Int = 0
)