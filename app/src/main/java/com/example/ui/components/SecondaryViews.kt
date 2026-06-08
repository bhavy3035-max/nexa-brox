package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.BrowserScreen
import com.example.ui.BrowserViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksView(viewModel: BrowserViewModel, modifier: Modifier = Modifier) {
    val bookmarks by viewModel.bookmarks.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Bookmarks Manager", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.setScreen(BrowserScreen.Browser) }) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        if (bookmarks.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Bookmarks saved yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(bookmarks) { bookmark ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .clickable {
                                viewModel.loadUrl(bookmark.url)
                                viewModel.setScreen(BrowserScreen.Browser)
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bookmark.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(bookmark.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { viewModel.removeBookmark(bookmark) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Bookmark", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryView(viewModel: BrowserViewModel, modifier: Modifier = Modifier) {
    val history by viewModel.history.collectAsState()
    val recentlyClosed by viewModel.recentlyClosedTabs.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabMode by remember { mutableStateOf(0) } // 0 = Browsing History, 1 = Recently Closed Tabs

    val filteredHistory = if (searchQuery.isBlank()) {
        history
    } else {
        history.filter { it.title.contains(searchQuery, ignoreCase = true) || it.url.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Browsing History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row {
                if (selectedTabMode == 0 && filteredHistory.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearAllHistory() }) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                } else if (selectedTabMode == 1 && recentlyClosed.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearRecentlyClosedTabs() }) {
                        Text("Clear Recent", color = MaterialTheme.colorScheme.error)
                    }
                }
                IconButton(onClick = { viewModel.setScreen(BrowserScreen.Browser) }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }

        // Sub-tabs for toggling between standard history and recently closed tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            Button(
                onClick = { selectedTabMode = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTabMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (selectedTabMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = null,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Browsing History", fontSize = 12.sp)
            }
            
            Button(
                onClick = { selectedTabMode = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTabMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (selectedTabMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = null,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.RestorePage, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Recently Closed (${recentlyClosed.size})", fontSize = 12.sp)
            }
        }

        if (selectedTabMode == 0) {
            // Standard Browsing History Tab Screen
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search history...") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            if (filteredHistory.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No History matching standard query.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(filteredHistory) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    viewModel.loadUrl(item.url)
                                    viewModel.setScreen(BrowserScreen.Browser)
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                                    Text(sdf.format(Date(item.timestamp)), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                }
                                IconButton(onClick = { viewModel.removeHistoryItem(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Recently Closed Tabs Screen
            if (recentlyClosed.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Tab, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No recently closed tabs to restore.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(recentlyClosed) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    viewModel.restoreRecentlyClosedTab(item.id)
                                    viewModel.setScreen(BrowserScreen.Browser)
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = {
                                    viewModel.restoreRecentlyClosedTab(item.id)
                                    viewModel.setScreen(BrowserScreen.Browser)
                                }) {
                                    Icon(Icons.Default.SettingsBackupRestore, contentDescription = "Restore Tab", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordsView(viewModel: BrowserViewModel, modifier: Modifier = Modifier) {
    val passwords by viewModel.passwords.collectAsState()
    var inputSite by remember { mutableStateOf("") }
    var inputUsername by remember { mutableStateOf("") }
    var inputPassword by remember { mutableStateOf("") }

    var isAddingPassword by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Saved Passwords", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = { isAddingPassword = !isAddingPassword }) {
                    Icon(if (isAddingPassword) Icons.Default.Close else Icons.Default.Add, contentDescription = "Add Password")
                }
                IconButton(onClick = { viewModel.setScreen(BrowserScreen.Browser) }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }

        if (isAddingPassword) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add Password Record Manually", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = inputSite, onValueChange = { inputSite = it }, label = { Text("Site URL/Domain") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = inputUsername, onValueChange = { inputUsername = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = inputPassword, onValueChange = { inputPassword = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = {
                            if (inputSite.isNotBlank() && inputUsername.isNotBlank() && inputPassword.isNotBlank()) {
                                viewModel.savePasswordDirectly(inputUsername, inputSite, inputPassword)
                                inputSite = ""
                                inputUsername = ""
                                inputPassword = ""
                                isAddingPassword = false
                            }
                        }) {
                            Text("Save Password")
                        }
                    }
                }
            }
        }

        if (passwords.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Password, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Passwords saved yet under this profile.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(passwords) { credentials ->
                    var isPasswordVisible by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(credentials.site, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("User: ${credentials.username}", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isPasswordVisible) credentials.password else "••••••••",
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        modifier = Modifier.size(14.dp).clickable { isPasswordVisible = !isPasswordVisible },
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.removePassword(credentials) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete record", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsView(viewModel: BrowserViewModel, modifier: Modifier = Modifier) {
    val downloads by viewModel.downloads.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Downloads Manager", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.setScreen(BrowserScreen.Browser) }) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        if (downloads.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No file downloads found.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(downloads) { download ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(download.fileName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(download.url, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { viewModel.deleteDownloadItem(download) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete download task", tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Status: ${download.status} (${download.progress}%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (download.status == "Completed") Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                )
                                Text(download.filePath, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }

                            LinearProgressIndicator(
                                progress = download.progress / 100f,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                color = if (download.status == "Completed") Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(viewModel: BrowserViewModel, modifier: Modifier = Modifier) {
    val searchEngine by viewModel.currentSearchEngine.collectAsState()
    val activeProfile by viewModel.currentProfile.collectAsState()
    val adBlockEnabled by viewModel.adBlockerEnabled.collectAsState()
    val popupBlockEnabled by viewModel.popupBlockerEnabled.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Settings & Privacy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.setScreen(BrowserScreen.Browser) }) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
            // Section 1: Profiles
            item {
                Text("Browser Profile Management", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Active Profile Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Each profile maintains its isolated sandbox of tabs, passwords and history log logs.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        val profiles = listOf("Personal", "Work", "Guest")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            profiles.forEach { p ->
                                val isSel = activeProfile == p
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { viewModel.setProfile(p) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(p, fontWeight = FontWeight.Bold, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Search Provider
            item {
                Text("Search Provider Engines", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Default Search Provider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Address bar queries will be routed via the selected provider.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(10.dp))

                        val engines = listOf("Google", "Bing", "DuckDuckGo")
                        engines.forEach { eng ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setSearchEngine(eng) }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(eng, fontWeight = FontWeight.Medium)
                                if (searchEngine == eng) {
                                    Icon(Icons.Default.Check, contentDescription = "Active Provider", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Guard/Shields settings
            item {
                Text("Guards & Shield Controls", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Shield Plus Ad Blocker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Block intrusive banner ads and tracking engines inside standard sites.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                            Switch(checked = adBlockEnabled, onCheckedChange = { viewModel.setAdBlockerEnabled(it) })
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Popup Interceptor Blocker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Intercept and suppress unrequested popups, maintaining flow safety.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                            Switch(checked = popupBlockEnabled, onCheckedChange = { viewModel.setPopupBlockerEnabled(it) })
                        }
                    }
                }
            }

            // Section 4: Privacy & Incognito Sandbox
            item {
                Text("Privacy & Incognito Sandbox", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Incognito Mode (Active Tab)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Browsing in private isolation doesn't record searches or page history locally.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                            Switch(
                                checked = activeTab?.isIncognito == true,
                                onCheckedChange = { _ -> viewModel.toggleIncognitoMode() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFFFB4AB),
                                    checkedTrackColor = Color(0xFF381F6D)
                                )
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Auto-Clear Private Session",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "✓ Active: Closing private tabs automatically purges local cached objects, session indices, and cookies.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
