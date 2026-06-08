package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserDao {

    // --- Bookmarks ---
    @Query("SELECT * FROM bookmarks WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getBookmarks(profileId: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url AND profileId = :profileId LIMIT 1)")
    suspend fun isBookmarked(url: String, profileId: String): Boolean

    @Query("DELETE FROM bookmarks WHERE url = :url AND profileId = :profileId")
    suspend fun deleteBookmarkByUrl(url: String, profileId: String)


    // --- History ---
    @Query("SELECT * FROM history WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getHistory(profileId: String): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Delete
    suspend fun deleteHistory(history: HistoryEntity)

    @Query("DELETE FROM history WHERE profileId = :profileId")
    suspend fun clearHistory(profileId: String)

    @Query("SELECT * FROM history WHERE profileId = :profileId AND (title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    suspend fun searchHistory(profileId: String, query: String): List<HistoryEntity>


    // --- Passwords ---
    @Query("SELECT * FROM passwords WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getPasswords(profileId: String): Flow<List<PasswordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(password: PasswordEntity)

    @Delete
    suspend fun deletePassword(password: PasswordEntity)

    @Query("SELECT * FROM passwords WHERE site LIKE '%' || :site || '%' AND profileId = :profileId LIMIT 1")
    suspend fun getPasswordForSite(site: String, profileId: String): PasswordEntity?


    // --- Downloads ---
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getDownloads(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long

    @Query("UPDATE downloads SET progress = :progress, status = :status WHERE id = :id")
    suspend fun updateDownloadProgress(id: Int, progress: Int, status: String)

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)


    // --- Tabs ---
    @Query("SELECT * FROM tabs WHERE profileId = :profileId ORDER BY timestamp ASC")
    fun getTabs(profileId: String): Flow<List<TabEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTab(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun deleteTab(id: String)

    @Query("DELETE FROM tabs WHERE profileId = :profileId")
    suspend fun clearTabs(profileId: String)
}
