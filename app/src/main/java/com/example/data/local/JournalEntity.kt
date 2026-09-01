package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.ConversationTurn
import com.example.data.model.JournalEntry
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val turnListType = Types.newParameterizedType(List::class.java, ConversationTurn::class.java)
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)

    private val turnAdapter = moshi.adapter<List<ConversationTurn>>(turnListType)
    private val stringAdapter = moshi.adapter<List<String>>(stringListType)

    @TypeConverter
    fun fromTurnList(turns: List<ConversationTurn>?): String {
        return turnAdapter.toJson(turns ?: emptyList())
    }

    @TypeConverter
    fun toTurnList(json: String?): List<ConversationTurn> {
        return if (json.isNullOrBlank()) emptyList() else turnAdapter.fromJson(json) ?: emptyList()
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return stringAdapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        return if (json.isNullOrBlank()) emptyList() else stringAdapter.fromJson(json) ?: emptyList()
    }
}

@Entity(tableName = "journal_entries")
@TypeConverters(Converters::class)
data class JournalEntity(
    @PrimaryKey val id: String,
    val userId: String, // Critical: user isolation partition key
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
) {
    fun toModel(): JournalEntry = JournalEntry(
        id = id,
        userId = userId,
        title = title,
        prompt = prompt,
        response = response,
        mode = mode,
        modelUsed = modelUsed,
        createdAt = createdAt,
        turns = turns,
        tags = tags,
        isPinned = isPinned,
        sentiment = sentiment
    )

    companion object {
        fun fromModel(model: JournalEntry): JournalEntity = JournalEntity(
            id = model.id,
            userId = model.userId,
            title = model.title,
            prompt = model.prompt,
            response = model.response,
            mode = model.mode,
            modelUsed = model.modelUsed,
            createdAt = model.createdAt,
            turns = model.turns,
            tags = model.tags,
            isPinned = model.isPinned,
            sentiment = model.sentiment
        )
    }
}
