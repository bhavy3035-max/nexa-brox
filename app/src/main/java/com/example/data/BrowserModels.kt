package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis(),
    val profileId: String = "Personal"
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis(),
    val profileId: String = "Personal"
)

@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val site: String,
    val username: String,
    val password: String,
    val timestamp: Long = System.currentTimeMillis(),
    val profileId: String = "Personal"
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val url: String,
    val filePath: String,
    val fileSize: Long = 0L,
    val progress: Int = 0, // 0 to 100
    val status: String = "Downloading", // "Downloading", "Completed", "Failed"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val isIncognito: Boolean = false,
    val tabGroup: String? = null,
    val tabGroupColor: Int? = null, // ARGB value or null
    val timestamp: Long = System.currentTimeMillis(),
    val profileId: String = "Personal"
)
