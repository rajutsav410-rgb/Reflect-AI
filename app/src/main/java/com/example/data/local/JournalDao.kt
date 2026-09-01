package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    // Strictly isolate entries by userId
    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY isPinned DESC, createdAt DESC")
    fun getEntriesForUser(userId: String): Flow<List<JournalEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :entryId AND userId = :userId LIMIT 1")
    suspend fun getEntryById(entryId: String, userId: String): JournalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: JournalEntity)

    @Query("DELETE FROM journal_entries WHERE id = :entryId AND userId = :userId")
    suspend fun deleteEntry(entryId: String, userId: String)

    @Query("DELETE FROM journal_entries WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
