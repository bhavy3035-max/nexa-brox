package com.example.data

import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val browserDao: BrowserDao) {

    // --- Bookmarks ---
    fun getBookmarks(profileId: String): Flow<List<BookmarkEntity>> =
        browserDao.getBookmarks(profileId)

    suspend fun insertBookmark(bookmark: BookmarkEntity) =
        browserDao.insertBookmark(bookmark)

    suspend fun deleteBookmark(bookmark: BookmarkEntity) =
        browserDao.deleteBookmark(bookmark)

    suspend fun isBookmarked(url: String, profileId: String): Boolean =
        browserDao.isBookmarked(url, profileId)

    suspend fun deleteBookmarkByUrl(url: String, profileId: String) =
        browserDao.deleteBookmarkByUrl(url, profileId)


    // --- History ---
    fun getHistory(profileId: String): Flow<List<HistoryEntity>> =
        browserDao.getHistory(profileId)

    suspend fun insertHistory(history: HistoryEntity) =
        browserDao.insertHistory(history)

    suspend fun deleteHistory(history: HistoryEntity) =
        browserDao.deleteHistory(history)

    suspend fun clearHistory(profileId: String) =
        browserDao.clearHistory(profileId)

    suspend fun searchHistory(profileId: String, query: String): List<HistoryEntity> =
        browserDao.searchHistory(profileId, query)


    // --- Passwords ---
    fun getPasswords(profileId: String): Flow<List<PasswordEntity>> =
        browserDao.getPasswords(profileId)

    suspend fun insertPassword(password: PasswordEntity) =
        browserDao.insertPassword(password)

    suspend fun deletePassword(password: PasswordEntity) =
        browserDao.deletePassword(password)

    suspend fun getPasswordForSite(site: String, profileId: String): PasswordEntity? =
        browserDao.getPasswordForSite(site, profileId)


    // --- Downloads ---
    fun getDownloads(): Flow<List<DownloadEntity>> =
        browserDao.getDownloads()

    suspend fun insertDownload(download: DownloadEntity): Long =
        browserDao.insertDownload(download)

    suspend fun updateDownloadProgress(id: Int, progress: Int, status: String) =
        browserDao.updateDownloadProgress(id, progress, status)

    suspend fun deleteDownload(download: DownloadEntity) =
        browserDao.deleteDownload(download)


    // --- Tabs ---
    fun getTabs(profileId: String): Flow<List<TabEntity>> =
        browserDao.getTabs(profileId)

    suspend fun saveTab(tab: TabEntity) =
        browserDao.saveTab(tab)

    suspend fun deleteTab(id: String) =
        browserDao.deleteTab(id)

    suspend fun clearTabs(profileId: String) =
        browserDao.clearTabs(profileId)
}
