package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserSession
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application.applicationContext)
    val currentUser: StateFlow<UserSession?> = repository.currentUser

    fun signInPreset(user: UserSession) {
        viewModelScope.launch {
            repository.signInWithProfile(
                uid = user.uid,
                email = user.email,
                displayName = user.displayName,
                photoUrl = user.photoUrl,
                provider = user.authProvider
            )
        }
    }

    fun signInCustom(email: String, name: String) {
        val safeEmail = email.trim()
        val safeName = name.trim().ifBlank { safeEmail.substringBefore("@").replaceFirstChar { it.uppercase() } }
        val uid = "user_" + safeEmail.replace("@", "_").replace(".", "_")
        viewModelScope.launch {
            repository.signInWithProfile(
                uid = uid,
                email = safeEmail,
                displayName = safeName,
                provider = "Google Account"
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }
}
