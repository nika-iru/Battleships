package com.fuentes.battleships.modules.auth.data

data class User(
    val id: String,
    val username: String,
    val email: String?,
    val wins: Int = 0,
)