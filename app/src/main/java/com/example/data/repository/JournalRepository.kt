package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.JournalEntity
import com.example.data.model.ConversationTurn
import com.example.data.model.JournalEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class JournalRepository(context: Context) {
    private val TAG = "JournalRepository"
    private val database = AppDatabase.getDatabase(context)
    private val journalDao = database.journalDao()
    private val ioScope = CoroutineScope(Dispatchers.IO)

    /**
     * Observe entries strictly isolated by userId.
     */
    fun getEntriesForUser(userId: String): Flow<List<JournalEntry>> {
        return journalDao.getEntriesForUser(userId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Saves entry to local Room database and syncs to Cloud Firestore at:
     * /users/{userId}/interactions/{interactionId}
     */
    suspend fun saveEntry(entry: JournalEntry) {
        // 1. Guaranteed local persistence (zero data loss)
        journalDao.insertOrUpdate(JournalEntity.fromModel(entry))

        // 2. Cloud Firestore remote synchronization
        ioScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val docRef = firestore.collection("users")
                    .document(entry.userId)
                    .collection("interactions")
                    .document(entry.id)

                val dataMap = hashMapOf(
                    "id" to entry.id,
                    "userId" to entry.userId,
                    "title" to entry.title,
                    "prompt" to entry.prompt,
                    "response" to entry.response,
                    "mode" to entry.mode,
                    "modelUsed" to entry.modelUsed,
                    "createdAt" to entry.createdAt,
                    "tags" to entry.tags,
                    "isPinned" to entry.isPinned,
                    "sentiment" to entry.sentiment
                )

                docRef.set(dataMap, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully synced entry ${entry.id} to Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Firestore sync deferred/offline: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.d(TAG, "Firestore unavailable or not configured in current sandbox: ${e.message}")
            }
        }
    }

    suspend fun addTurnToEntry(entryId: String, userId: String, turn: ConversationTurn) {
        val existing = journalDao.getEntryById(entryId, userId) ?: return
        val currentModel = existing.toModel()
        val updatedTurns = currentModel.turns + turn
        val updatedModel = currentModel.copy(turns = updatedTurns)
        saveEntry(updatedModel)
    }

    suspend fun togglePin(entryId: String, userId: String) {
        val existing = journalDao.getEntryById(entryId, userId) ?: return
        val currentModel = existing.toModel()
        val updated = currentModel.copy(isPinned = !currentModel.isPinned)
        saveEntry(updated)
    }

    suspend fun deleteEntry(entryId: String, userId: String) {
        journalDao.deleteEntry(entryId, userId)

        ioScope.launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("interactions")
                    .document(entryId)
                    .delete()
            } catch (e: Exception) {
                Log.d(TAG, "Firestore delete deferred: ${e.message}")
            }
        }
    }
}
