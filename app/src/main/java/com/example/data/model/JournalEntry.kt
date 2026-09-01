package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConversationTurn(
    val id: String,
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long,
    val model: String = "gemini-3.5-flash"
)

@JsonClass(generateAdapter = true)
data class JournalEntry(
    val id: String,
    val userId: String,
    val title: String,
    val prompt: String,
    val response: String,
    val mode: String,
    val modelUsed: String,
    val createdAt: Long,
    val turns: List<ConversationTurn> = emptyList(),
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val sentiment: String = "Mindful"
)
