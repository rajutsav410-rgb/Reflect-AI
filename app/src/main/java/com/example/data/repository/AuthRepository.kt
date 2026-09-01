package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.UserSession
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserSession?>(null)
    val currentUser: StateFlow<UserSession?> = _currentUser.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val uid = prefs.getString("user_uid", null)
        val email = prefs.getString("user_email", null)
        val name = prefs.getString("user_name", null)
        val photoUrl = prefs.getString("user_photo", null)
        val provider = prefs.getString("user_provider", "Google Sign-In")

        if (!uid.isNullOrBlank() && !email.isNullOrBlank()) {
            _currentUser.value = UserSession(
                uid = uid,
                email = email,
                displayName = name ?: email.substringBefore("@").replaceFirstChar { it.uppercase() },
                photoUrl = photoUrl,
                authProvider = provider ?: "Google Sign-In"
            )
        } else {
            // Check Firebase Auth instance if initialized
            try {
                val fbUser = FirebaseAuth.getInstance().currentUser
                if (fbUser != null) {
                    _currentUser.value = UserSession(
                        uid = fbUser.uid,
                        email = fbUser.email ?: "user@gmail.com",
                        displayName = fbUser.displayName ?: "Firebase User",
                        photoUrl = fbUser.photoUrl?.toString(),
                        authProvider = "Firebase Auth"
                    )
                }
            } catch (e: Exception) {
                Log.d("AuthRepository", "Firebase auth not initialized yet: ${e.message}")
            }
        }
    }

    fun signInWithProfile(uid: String, email: String, displayName: String, photoUrl: String? = null, provider: String = "Google Sign-In") {
        prefs.edit()
            .putString("user_uid", uid)
            .putString("user_email", email)
            .putString("user_name", displayName)
            .putString("user_photo", photoUrl)
            .putString("user_provider", provider)
            .apply()

        _currentUser.value = UserSession(
            uid = uid,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            authProvider = provider
        )
    }

    fun signOut() {
        prefs.edit().clear().apply()
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}
        _currentUser.value = null
    }

    companion object {
        // Predefined testing profiles demonstrating strict multi-user isolation
        val PRESET_ALICE = UserSession(
            uid = "user_alice_9981",
            email = "alice.chen@gmail.com",
            displayName = "Alice Chen",
            photoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
            authProvider = "Google Account"
        )
        val PRESET_BOB = UserSession(
            uid = "user_bob_4412",
            email = "bob.marley@gmail.com",
            displayName = "Bob Dylan",
            photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
            authProvider = "Google Account"
        )
        val PRESET_CAROL = UserSession(
            uid = "user_carol_7723",
            email = "carol.danvers@gmail.com",
            displayName = "Carol Danvers",
            photoUrl = "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=150",
            authProvider = "Google Account"
        )
    }
}
