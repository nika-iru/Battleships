package com.fuentes.battleships.modules.auth.ui

data class AuthState(
    val email: String? = "",
    val password: String = "",
    val userId: String? = "",
    val username: String? = null
)