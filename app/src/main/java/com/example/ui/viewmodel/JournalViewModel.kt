package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.model.ConversationTurn
import com.example.data.model.JournalEntry
import com.example.data.model.ReflectionMode
import com.example.data.repository.JournalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface GenerationUiState {
    object Idle : GenerationUiState
    data class Generating(val prompt: String, val mode: ReflectionMode) : GenerationUiState
    data class Success(val entry: JournalEntry) : GenerationUiState
    data class Error(val message: String, val canRetry: Boolean = true) : GenerationUiState
}

class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = JournalRepository(application.applicationContext)

    private val _currentUserId = MutableStateFlow<String?>(null)

    // Composer Input State
    val titleInput = MutableStateFlow("")
    val promptInput = MutableStateFlow("")
    val selectedMode = MutableStateFlow(ReflectionMode.REFLECTION)
    val selectedSentiment = MutableStateFlow("Mindful")

    // Generation State
    private val _generationState = MutableStateFlow<GenerationUiState>(GenerationUiState.Idle)
    val generationState: StateFlow<GenerationUiState> = _generationState.asStateFlow()

    // Follow-up chat state for active conversation
    val followUpInput = MutableStateFlow("")
    private val _isFollowUpLoading = MutableStateFlow(false)
    val isFollowUpLoading: StateFlow<Boolean> = _isFollowUpLoading.asStateFlow()

    // Search and filter state
    val searchQuery = MutableStateFlow("")
    val filterMode = MutableStateFlow<ReflectionMode?>(null)

    // Active selected entry for detailed view or multi-turn interaction
    val selectedEntry = MutableStateFlow<JournalEntry?>(null)

    // Strictly isolated user journal entries stream
    val entries: StateFlow<List<JournalEntry>> = _currentUserId
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(emptyList())
            else repository.getEntriesForUser(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered entries stream
    val filteredEntries: StateFlow<List<JournalEntry>> = combine(
        entries,
        searchQuery,
        filterMode
    ) { allEntries, query, modeFilter ->
        allEntries.filter { entry ->
            val matchesQuery = query.isBlank() ||
                    entry.title.contains(query, ignoreCase = true) ||
                    entry.prompt.contains(query, ignoreCase = true) ||
                    entry.response.contains(query, ignoreCase = true)

            val matchesMode = modeFilter == null || entry.mode == modeFilter.title

            matchesQuery && matchesMode
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setUserId(userId: String?) {
        if (_currentUserId.value != userId) {
            _currentUserId.value = userId
            _generationState.value = GenerationUiState.Idle
            selectedEntry.value = null
        }
    }

    fun selectPresetPrompt(promptText: String, mode: ReflectionMode) {
        promptInput.value = promptText
        selectedMode.value = mode
        if (titleInput.value.isBlank()) {
            titleInput.value = promptText.take(40) + if (promptText.length > 40) "..." else ""
        }
    }

    /**
     * Sends prompt to Gemini API with fallback ladder, and persists result to isolated Firestore & Room.
     */
    fun submitJournalEntry() {
        val uid = _currentUserId.value ?: return
        val prompt = promptInput.value.trim()
        if (prompt.isBlank()) return

        val title = titleInput.value.trim().ifBlank {
            prompt.take(35).trim() + "..."
        }
        val mode = selectedMode.value
        val sentiment = selectedSentiment.value

        _generationState.value = GenerationUiState.Generating(prompt, mode)

        viewModelScope.launch {
            val result = GeminiClient.generateReflection(
                prompt = prompt,
                modePrefix = mode.promptPrefix
            )

            result.onSuccess { (responseText, modelUsed) ->
                val newEntry = JournalEntry(
                    id = UUID.randomUUID().toString(),
                    userId = uid,
                    title = title,
                    prompt = prompt,
                    response = responseText,
                    mode = mode.title,
                    modelUsed = modelUsed,
                    createdAt = System.currentTimeMillis(),
                    turns = emptyList(),
                    tags = listOf(mode.title, sentiment),
                    isPinned = false,
                    sentiment = sentiment
                )

                repository.saveEntry(newEntry)
                _generationState.value = GenerationUiState.Success(newEntry)
                selectedEntry.value = newEntry

                // Clear composer fields only after verified persistence
                titleInput.value = ""
                promptInput.value = ""
            }.onFailure { error ->
                _generationState.value = GenerationUiState.Error(
                    message = error.message ?: "Failed to generate reflection. Please try again."
                )
            }
        }
    }

    /**
     * Multi-turn conversational follow-up on an existing reflection entry.
     */
    fun sendFollowUp(entryId: String) {
        val uid = _currentUserId.value ?: return
        val followUpText = followUpInput.value.trim()
        if (followUpText.isBlank() || _isFollowUpLoading.value) return

        val currentEntry = selectedEntry.value ?: return
        if (currentEntry.id != entryId) return

        _isFollowUpLoading.value = true
        val userTurn = ConversationTurn(
            id = UUID.randomUUID().toString(),
            role = "user",
            text = followUpText,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            // Build conversation history including initial entry and prior turns
            val history = mutableListOf<ConversationTurn>()
            history.add(
                ConversationTurn(
                    id = "init-user",
                    role = "user",
                    text = currentEntry.prompt,
                    timestamp = currentEntry.createdAt
                )
            )
            history.add(
                ConversationTurn(
                    id = "init-model",
                    role = "model",
                    text = currentEntry.response,
                    timestamp = currentEntry.createdAt + 1000
                )
            )
            history.addAll(currentEntry.turns)

            val result = GeminiClient.generateReflection(
                prompt = followUpText,
                modePrefix = "Continue the conversation concisely and helpfully:",
                historyTurns = history
            )

            result.onSuccess { (modelReply, modelUsed) ->
                val modelTurn = ConversationTurn(
                    id = UUID.randomUUID().toString(),
                    role = "model",
                    text = modelReply,
                    timestamp = System.currentTimeMillis(),
                    model = modelUsed
                )

                val updatedTurns = currentEntry.turns + userTurn + modelTurn
                val updatedEntry = currentEntry.copy(turns = updatedTurns)
                repository.saveEntry(updatedEntry)
                selectedEntry.value = updatedEntry
                followUpInput.value = ""
                _isFollowUpLoading.value = false
            }.onFailure {
                _isFollowUpLoading.value = false
            }
        }
    }

    fun togglePin(entryId: String) {
        val uid = _currentUserId.value ?: return
        viewModelScope.launch {
            repository.togglePin(entryId, uid)
            if (selectedEntry.value?.id == entryId) {
                selectedEntry.value = selectedEntry.value?.let { it.copy(isPinned = !it.isPinned) }
            }
        }
    }

    fun deleteEntry(entryId: String) {
        val uid = _currentUserId.value ?: return
        viewModelScope.launch {
            repository.deleteEntry(entryId, uid)
            if (selectedEntry.value?.id == entryId) {
                selectedEntry.value = null
            }
        }
    }

    fun resetGenerationState() {
        _generationState.value = GenerationUiState.Idle
    }
}
