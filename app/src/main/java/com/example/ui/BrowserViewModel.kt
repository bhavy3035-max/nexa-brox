package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.RetrofitClient
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI
import java.util.UUID

sealed interface BrowserScreen {
    object Browser : BrowserScreen
    object History : BrowserScreen
    object Bookmarks : BrowserScreen
    object Passwords : BrowserScreen
    object Downloads : BrowserScreen
    object Settings : BrowserScreen
}

data class TabState(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Tab",
    val url: String = "about:blank",
    val isIncognito: Boolean = false,
    val tabGroup: String? = null,
    val tabGroupColor: Int? = null
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class RecentlyClosedTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BrowserDatabase.getDatabase(application)
    private val repository = BrowserRepository(database.browserDao())

    // --- Active Configuration State ---
    private val _currentProfile = MutableStateFlow("Personal")
    val currentProfile: StateFlow<String> = _currentProfile.asStateFlow()

    private val _currentSearchEngine = MutableStateFlow("Google")
    val currentSearchEngine: StateFlow<String> = _currentSearchEngine.asStateFlow()

    private val _adBlockerEnabled = MutableStateFlow(true)
    val adBlockerEnabled: StateFlow<Boolean> = _adBlockerEnabled.asStateFlow()

    private val _popupBlockerEnabled = MutableStateFlow(true)
    val popupBlockerEnabled: StateFlow<Boolean> = _popupBlockerEnabled.asStateFlow()

    private val _currentScreen = MutableStateFlow<BrowserScreen>(BrowserScreen.Browser)
    val currentScreen: StateFlow<BrowserScreen> = _currentScreen.asStateFlow()

    // --- Web Navigation States ---
    private val _inputUrl = MutableStateFlow("")
    val inputUrl: StateFlow<String> = _inputUrl.asStateFlow()

    private val _currentLoadedUrl = MutableStateFlow("https://www.google.com")
    val currentLoadedUrl: StateFlow<String> = _currentLoadedUrl.asStateFlow()

    private val _pageTitle = MutableStateFlow("Web Browser")
    val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()

    private val _webProgress = MutableStateFlow(0)
    val webProgress: StateFlow<Int> = _webProgress.asStateFlow()

    private val _isWebLoading = MutableStateFlow(false)
    val isWebLoading: StateFlow<Boolean> = _isWebLoading.asStateFlow()

    private val _isSecureHttps = MutableStateFlow(true)
    val isSecureHttps: StateFlow<Boolean> = _isSecureHttps.asStateFlow()

    // --- Tabs System ---
    private val _tabs = MutableStateFlow<List<TabState>>(listOf(TabState(url = "https://www.google.com")))
    val tabs: StateFlow<List<TabState>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String>(_tabs.value.first().id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    // --- Recently Closed Tabs ---
    private val _recentlyClosedTabs = MutableStateFlow<List<RecentlyClosedTab>>(emptyList())
    val recentlyClosedTabs: StateFlow<List<RecentlyClosedTab>> = _recentlyClosedTabs.asStateFlow()

    // --- Navigation Fast Events ---
    private val _navigationEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigationEvents = _navigationEvents.asSharedFlow()

    val activeTab: StateFlow<TabState?> = combine(_tabs, _activeTabId) { tabsList, activeId ->
        tabsList.find { it.id == activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _tabs.value.first())

    // --- Smart Tab Groups ---
    private val _tabGroups = MutableStateFlow<List<String>>(listOf("Work", "Social", "Finance", "Entertainment"))
    val tabGroups: StateFlow<List<String>> = _tabGroups.asStateFlow()

    // --- DB reactive flows ---
    val bookmarks = _currentProfile.flatMapLatest { profile ->
        repository.getBookmarks(profile)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history = _currentProfile.flatMapLatest { profile ->
        repository.getHistory(profile)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val passwords = _currentProfile.flatMapLatest { profile ->
        repository.getPasswords(profile)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads = repository.getDownloads().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- AI Assistant Integration ---
    private val _aiMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(text = "Hello! I am your AI Web Copilot. Ask me anything about the active page, summarize its content, or try translations!", isFromUser = false)
    ))
    val aiMessages: StateFlow<List<ChatMessage>> = _aiMessages.asStateFlow()

    private val _aiWebpageSummary = MutableStateFlow<String?>(null)
    val aiWebpageSummary: StateFlow<String?> = _aiWebpageSummary.asStateFlow()

    private val _securityReport = MutableStateFlow<String?>(null)
    val securityReport: StateFlow<String?> = _securityReport.asStateFlow()

    private val _isSecurityChecking = MutableStateFlow(false)
    val isSecurityChecking: StateFlow<Boolean> = _isSecurityChecking.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    var extractedPageContent: String = ""

    // --- Password Manager Prompts ---
    private val _pendingPasswordSave = MutableStateFlow<PasswordEntity?>(null)
    val pendingPasswordSave: StateFlow<PasswordEntity?> = _pendingPasswordSave.asStateFlow()

    // --- Reading Mode state ---
    private val _readerModeActive = MutableStateFlow(false)
    val readerModeActive: StateFlow<Boolean> = _readerModeActive.asStateFlow()

    private val _readerContentHtml = MutableStateFlow("")
    val readerContentHtml: StateFlow<String> = _readerContentHtml.asStateFlow()

    // --- Screenshot state ---
    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    // --- Incognito Session Triggers ---
    private val _clearSessionEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val clearSessionEvents = _clearSessionEvents.asSharedFlow()

    init {
        // Load initial tabs from database to restore sessions
        viewModelScope.launch {
            repository.getTabs(_currentProfile.value).firstOrNull()?.let { savedTabs ->
                if (savedTabs.isNotEmpty()) {
                    val loaded = savedTabs.map { entity ->
                        TabState(
                            id = entity.id,
                            title = entity.title,
                            url = entity.url,
                            isIncognito = entity.isIncognito,
                            tabGroup = entity.tabGroup,
                            tabGroupColor = entity.tabGroupColor
                        )
                    }
                    _tabs.value = loaded
                    _activeTabId.value = loaded.first().id
                }
            }
        }
    }

    // --- Navigation UI ---
    fun setScreen(screen: BrowserScreen) {
        _currentScreen.value = screen
    }

    fun setProfile(profile: String) {
        _currentProfile.value = profile
        // Clear active tabs and load profile tabs
        viewModelScope.launch {
            repository.getTabs(profile).firstOrNull()?.let { savedTabs ->
                if (savedTabs.isNotEmpty()) {
                    val loaded = savedTabs.map { entity ->
                        TabState(
                            id = entity.id,
                            title = entity.title,
                            url = entity.url,
                            isIncognito = entity.isIncognito,
                            tabGroup = entity.tabGroup,
                            tabGroupColor = entity.tabGroupColor
                        )
                    }
                    _tabs.value = loaded
                    _activeTabId.value = loaded.first().id
                } else {
                    val defaultTab = TabState(url = "https://www.google.com")
                    _tabs.value = listOf(defaultTab)
                    _activeTabId.value = defaultTab.id
                }
            }
        }
    }

    fun setSearchEngine(engine: String) {
        _currentSearchEngine.value = engine
    }

    fun setAdBlockerEnabled(enabled: Boolean) {
        _adBlockerEnabled.value = enabled
    }

    fun setPopupBlockerEnabled(enabled: Boolean) {
        _popupBlockerEnabled.value = enabled
    }

    // --- Address Bar URL processing --
    fun setInputUrl(url: String) {
        _inputUrl.value = url
    }

    fun formatAndLoadUrl(query: String) {
        val trimmed = query.trim()
        val searchPrefix = when (_currentSearchEngine.value) {
            "Google" -> "https://www.google.com/search?q="
            "Bing" -> "https://www.bing.com/search?q="
            "DuckDuckGo" -> "https://duckduckgo.com/?q="
            else -> "https://www.google.com/search?q="
        }

        val destinationUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else if (trimmed.contains(".") && !trimmed.contains(" ") && trimmed.length > 3) {
            "https://$trimmed"
        } else if (trimmed == "about:blank" || trimmed.startsWith("file://")) {
            trimmed
        } else {
            searchPrefix + java.net.URLEncoder.encode(trimmed, "UTF-8")
        }

        loadUrl(destinationUrl)
    }

    fun loadUrl(url: String) {
        _currentLoadedUrl.value = url
        _inputUrl.value = url
        _isSecureHttps.value = url.startsWith("https://")

        // Sync to current active tab state
        val updatedTabs = _tabs.value.map { tab ->
            if (tab.id == _activeTabId.value) {
                tab.copy(url = url, title = "Loading...")
            } else {
                tab
            }
        }
        _tabs.value = updatedTabs
        saveTabsToDb()

        // Fast WebView load execution
        viewModelScope.launch {
            _navigationEvents.emit(url)
        }
    }

    // --- WebView State Observers ---
    fun onPageStarted(url: String) {
        _isWebLoading.value = true
        _webProgress.value = 5
        _currentLoadedUrl.value = url
        _isSecureHttps.value = url.startsWith("https://")
    }

    fun onPageFinished(url: String, title: String) {
        _isWebLoading.value = false
        _webProgress.value = 100
        _currentLoadedUrl.value = url
        _pageTitle.value = title.ifBlank { url }

        // Update active tab title and URL
        val updatedTabs = _tabs.value.map { tab ->
            if (tab.id == _activeTabId.value) {
                tab.copy(url = url, title = if (title.isBlank()) url else title)
            } else {
                tab
            }
        }
        _tabs.value = updatedTabs
        saveTabsToDb()

        // Insert to history database only if NOT incognito
        activeTab.value?.let { currentTab ->
            if (!currentTab.isIncognito && url != "about:blank" && !url.startsWith("file://")) {
                viewModelScope.launch {
                    repository.insertHistory(
                        HistoryEntity(
                            title = title.ifBlank { url },
                            url = url,
                            profileId = _currentProfile.value
                        )
                    )
                }
            }
        }
    }

    fun onLoadingProgress(progress: Int) {
        _webProgress.value = progress
        if (progress == 100) {
            _isWebLoading.value = false
        }
    }

    // --- Tab Actions ---
    fun addNewTab(url: String = "https://www.google.com", isIncognito: Boolean = false) {
        val newTab = TabState(
            id = UUID.randomUUID().toString(),
            url = url,
            title = if (url == "https://www.google.com") "New Tab" else "Web Browser",
            isIncognito = isIncognito
        )
        val updated = _tabs.value.toMutableList()
        updated.add(newTab)
        _tabs.value = updated
        _activeTabId.value = newTab.id
        loadUrl(url)
        saveTabsToDb()
    }

    fun switchTab(tabId: String) {
        val target = _tabs.value.find { it.id == tabId }
        if (target != null) {
            _activeTabId.value = tabId
            _currentLoadedUrl.value = target.url
            _inputUrl.value = target.url
            _pageTitle.value = target.title
        }
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        val closedTab = currentTabs.find { it.id == tabId }
        closedTab?.let {
            if (!it.isIncognito && it.url != "about:blank" && !it.url.startsWith("file://")) {
                val recently = _recentlyClosedTabs.value.toMutableList()
                recently.add(0, RecentlyClosedTab(title = it.title, url = it.url))
                if (recently.size > 15) {
                    recently.removeLast()
                }
                _recentlyClosedTabs.value = recently
            } else if (it.isIncognito) {
                clearPrivateSession()
            }
            viewModelScope.launch {
                repository.deleteTab(tabId)
            }
        }

        if (currentTabs.size <= 1) {
            // Keep at least one tab open
            val defaultTab = TabState(url = "https://www.google.com")
            _tabs.value = listOf(defaultTab)
            _activeTabId.value = defaultTab.id
            loadUrl("https://www.google.com")
            saveTabsToDb()
            return
        }

        val updated = currentTabs.filter { it.id != tabId }
        _tabs.value = updated

        if (_activeTabId.value == tabId) {
            // Switch to previous or first tab
            val nextActiveId = updated.lastOrNull()?.id ?: updated.first().id
            _activeTabId.value = nextActiveId
            val nextTab = updated.find { it.id == nextActiveId }
            nextTab?.let {
                _currentLoadedUrl.value = it.url
                _inputUrl.value = it.url
                _pageTitle.value = it.title
            }
        }
        saveTabsToDb()
    }

    fun toggleIncognitoMode() {
        val updatedTabs = _tabs.value.map { tab ->
            if (tab.id == _activeTabId.value) {
                val nextState = !tab.isIncognito
                if (!nextState) {
                    clearPrivateSession()
                } else {
                    viewModelScope.launch(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            getApplication(),
                            "Incognito Sandbox Active: No history will be recorded",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                tab.copy(isIncognito = nextState)
            } else {
                tab
            }
        }
        _tabs.value = updatedTabs
        saveTabsToDb()
    }

    fun clearPrivateSession() {
        viewModelScope.launch(Dispatchers.Main) {
            _clearSessionEvents.emit(Unit)
            android.widget.Toast.makeText(
                getApplication(),
                "Private session caches & cookies cleared!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun restoreRecentlyClosedTab(id: String) {
        val target = _recentlyClosedTabs.value.find { it.id == id }
        if (target != null) {
            addNewTab(target.url)
            _recentlyClosedTabs.value = _recentlyClosedTabs.value.filter { it.id != id }
        }
    }

    fun clearRecentlyClosedTabs() {
        _recentlyClosedTabs.value = emptyList()
    }

    fun assignGroupToTab(tabId: String, groupName: String?, color: Int? = null) {
        val updated = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(tabGroup = groupName, tabGroupColor = color)
            } else {
                tab
            }
        }
        _tabs.value = updated
        saveTabsToDb()
    }

    private fun saveTabsToDb() {
        viewModelScope.launch {
            // Sync in background to database
            activeTab.value?.let { active ->
                repository.saveTab(
                    TabEntity(
                        id = active.id,
                        title = active.title,
                        url = active.url,
                        isIncognito = active.isIncognito,
                        tabGroup = active.tabGroup,
                        tabGroupColor = active.tabGroupColor,
                        profileId = _currentProfile.value
                    )
                )
            }
        }
    }

    // --- Bookmarks Management ---
    fun toggleBookmark(title: String, url: String) {
        viewModelScope.launch {
            val isBooked = repository.isBookmarked(url, _currentProfile.value)
            if (isBooked) {
                repository.deleteBookmarkByUrl(url, _currentProfile.value)
            } else {
                repository.insertBookmark(
                    BookmarkEntity(
                        title = title.ifBlank { url },
                        url = url,
                        profileId = _currentProfile.value
                    )
                )
            }
        }
    }

    suspend fun isCurrentPageBookmarked(): Boolean {
        return repository.isBookmarked(_currentLoadedUrl.value, _currentProfile.value)
    }

    fun removeBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmark)
        }
    }

    // --- History Management ---
    fun removeHistoryItem(item: HistoryEntity) {
        viewModelScope.launch {
            repository.deleteHistory(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory(_currentProfile.value)
        }
    }

    // --- Passwords Management ---
    fun savePasswordDirectly(username: String, site: String, pass: String) {
        viewModelScope.launch {
            repository.insertPassword(
                PasswordEntity(
                    site = site,
                    username = username,
                    password = pass,
                    profileId = _currentProfile.value
                )
            )
        }
    }

    fun removePassword(password: PasswordEntity) {
        viewModelScope.launch {
            repository.deletePassword(password)
        }
    }

    fun checkAndAutofillPasswords(url: String, onAutofillReady: (PasswordEntity) -> Unit) {
        viewModelScope.launch {
            try {
                val uri = URI(url)
                val domain = uri.host ?: ""
                repository.getPasswordForSite(domain, _currentProfile.value)?.let { passwordEntity ->
                    onAutofillReady(passwordEntity)
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
    }

    fun promptToSavePassword(username: String, pass: String, url: String) {
        try {
            val domain = URI(url).host ?: url
            _pendingPasswordSave.value = PasswordEntity(
                site = domain,
                username = username,
                password = pass,
                profileId = _currentProfile.value
            )
        } catch (e: Exception) {
            _pendingPasswordSave.value = PasswordEntity(
                site = url,
                username = username,
                password = pass,
                profileId = _currentProfile.value
            )
        }
    }

    fun confirmSavePassword() {
        _pendingPasswordSave.value?.let { saved ->
            viewModelScope.launch {
                repository.insertPassword(saved)
                _pendingPasswordSave.value = null
            }
        }
    }

    fun dismissSavePassword() {
        _pendingPasswordSave.value = null
    }

    // --- Downloads Management ---
    fun registerDownload(fileName: String, url: String) {
        viewModelScope.launch {
            val id = repository.insertDownload(
                DownloadEntity(
                    fileName = fileName,
                    url = url,
                    filePath = "/storage/emulated/0/Download/$fileName",
                    status = "Downloading"
                )
            )
            // Mock dynamic downloading progress
            mockDownloadProgress(id.toInt())
        }
    }

    private fun mockDownloadProgress(downloadId: Int) {
        viewModelScope.launch {
            for (progress in 10..100 step 15) {
                kotlinx.coroutines.delay(1000)
                repository.updateDownloadProgress(
                    downloadId,
                    if (progress > 100) 100 else progress,
                    if (progress >= 100) "Completed" else "Downloading"
                )
            }
        }
    }

    fun deleteDownloadItem(item: DownloadEntity) {
        viewModelScope.launch {
            repository.deleteDownload(item)
        }
    }

    // --- Copilot AI panel action handler ---
    fun sendCopilotMessage(userText: String) {
        if (userText.isBlank()) return

        val trimmedMsg = userText.trim()
        val currentMsgs = _aiMessages.value.toMutableList()
        currentMsgs.add(ChatMessage(text = trimmedMsg, isFromUser = true))
        _aiMessages.value = currentMsgs

        _isAiLoading.value = true

        viewModelScope.launch {
            // Build prompt co-pilot can handle contextually
            val activeUrl = _currentLoadedUrl.value
            val activeTitle = _pageTitle.value

            val contextSnippet = if (extractedPageContent.isNotBlank()) {
                "Active Webpage URL: $activeUrl\n" +
                "Title: $activeTitle\n" +
                "Content:\n" +
                "${extractedPageContent.take(2000)}\n---\n"
            } else {
                "Active Webpage URL: $activeUrl\n" +
                "Title: $activeTitle\n---\n"
            }

            val systemInstruction = "You are a helpful, smart AI co-pilot embedded inside an Android Web Browser. " +
                    "Use the context of the user's active page when relevant to provide quick, insightful answers, summary or translation co-piloting."

            val prompt = if (extractedPageContent.isNotBlank()) {
                "Based on the following webpage context in the browser, answer this user question:\n" +
                        "Question: $trimmedMsg\n\n" +
                        "Context:\n$contextSnippet"
            } else {
                trimmedMsg
            }

            val response = withContext(Dispatchers.IO) {
                RetrofitClient.generateAiResponse(prompt, systemInstruction)
            }

            _isAiLoading.value = false
            val updatedMsgs = _aiMessages.value.toMutableList()
            updatedMsgs.add(ChatMessage(text = response, isFromUser = false))
            _aiMessages.value = updatedMsgs
        }
    }

    fun summarizeCurrentPage() {
        _isAiLoading.value = true
        _aiWebpageSummary.value = "Generating bullet-point summary co-pilot..."

        viewModelScope.launch {
            val activeUrl = _currentLoadedUrl.value
            val activeTitle = _pageTitle.value

            val prompt = if (extractedPageContent.isNotBlank()) {
                "Please generate a clean, modern, bullet-point executive summary from this webpage content. Be concise and capture key findings.\n" +
                        "Webpage Title: $activeTitle\n" +
                        "URL: $activeUrl\n\n" +
                        "Content:\n${extractedPageContent.take(6000)}"
            } else {
                "Please summarize the purpose of this website based on its metadata. Website: $activeUrl ($activeTitle)"
            }

            val response = withContext(Dispatchers.IO) {
                RetrofitClient.generateAiResponse(prompt, "You are an expert webpage summarizer.")
            }

            _isAiLoading.value = false
            _aiWebpageSummary.value = response
        }
    }

    fun translateCurrentPage(targetLanguage: String) {
        // We can translate via Google Translate Wrapper
        val translatedWrapper = "https://translate.google.com/translate?sl=auto&tl=$targetLanguage&u=${_currentLoadedUrl.value}"
        loadUrl(translatedWrapper)
    }

    fun clearAiSummary() {
        _aiWebpageSummary.value = null
    }

    fun auditWebsiteSecurity() {
        val activeUrl = _currentLoadedUrl.value
        val activeTitle = _pageTitle.value
        _isSecurityChecking.value = true
        _securityReport.value = "Analyzing domain security & safety parameters..."
        
        viewModelScope.launch {
            val systemInstruction = "You are an expert AI Cyber Security, Privacy, and Safe Browsing Analyst. Provide objective trust ratings, risk categories, and clear guidelines."
            
            val prompt = "Perform a security, privacy, and safety audit on this website domain. " +
                    "Evaluate potential tracker types, spam index, data collection policies, SSL standard presence and overall trust indicators. " +
                    "Return a clear safety report list containing:\n" +
                    "1. Nexa Trust Score: [0-100 Rating]\n" +
                    "2. Risk Tier Index: [Low / Medium / High]\n" +
                    "3. Tracking & Privacy Check: [Insight on tracking cookies or practices]\n" +
                    "4. Security Recommendations: [Provide 3 security/privacy recommendations specifically for browsing this site safely]\n\n" +
                    "Website to analyze: $activeUrl ($activeTitle)\n" +
                    "Page Text Fragment:\n${extractedPageContent.take(1500)}"
                    
            val result = withContext(Dispatchers.IO) {
                RetrofitClient.generateAiResponse(prompt, systemInstruction)
            }
            _securityReport.value = result
            _isSecurityChecking.value = false
        }
    }

    fun clearSecurityReport() {
        _securityReport.value = null
    }

    // --- Reader Mode ---
    fun toggleReaderMode() {
        val nextState = !_readerModeActive.value
        _readerModeActive.value = nextState
        if (nextState) {
            _isAiLoading.value = true
            _readerContentHtml.value = "AI is polishing a readable layout for this page..."
            viewModelScope.launch {
                val activeTitle = _pageTitle.value
                val activeUrl = _currentLoadedUrl.value
                val text = extractedPageContent.take(6000)

                val prompt = "Extract the main readable article/text content of this webpage cleanly. Avoid ads, navbars or footers. " +
                        "Format it beautifully as easy-to-read Markdown. " +
                        "Webpage url: $activeUrl, Title: $activeTitle. Content snippet to read:\n$text"

                val cleanText = withContext(Dispatchers.IO) {
                    RetrofitClient.generateAiResponse(prompt, "You are a clean reading mode text extractor optimizer.")
                }

                _readerContentHtml.value = cleanText
                _isAiLoading.value = false
            }
        }
    }

    fun closeReaderMode() {
        _readerModeActive.value = false
    }

    // --- Screenshot Capture & Annotation Overlay ---
    fun triggerCapturedBitmap(bitmap: Bitmap?) {
        _capturedBitmap.value = bitmap
    }

    fun clearCapturedBitmap() {
        _capturedBitmap.value = null
    }
}
