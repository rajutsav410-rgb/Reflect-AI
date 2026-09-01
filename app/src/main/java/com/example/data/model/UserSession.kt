package com.example.data.model

data class UserSession(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val authProvider: String = "Google Sign-In",
    val isAuthenticated: Boolean = true
)
