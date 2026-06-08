package com.example

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.RecognizerIntent
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainBrowserApp()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainBrowserApp() {
    val viewModel: BrowserViewModel = viewModel()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val profiles by viewModel.currentProfile.collectAsState()

    // Web state indicators
    val inputUrl by viewModel.inputUrl.collectAsState()
    val loadedUrl by viewModel.currentLoadedUrl.collectAsState()
    val pageTitle by viewModel.pageTitle.collectAsState()
    val webProgress by viewModel.webProgress.collectAsState()
    val isWebLoading by viewModel.isWebLoading.collectAsState()
    val isSecureHttps by viewModel.isSecureHttps.collectAsState()

    // Sub sheet toggles
    var isTabSelectorOpen by remember { mutableStateOf(false) }
    var isAiPanelOpen by remember { mutableStateOf(false) }
    var isMenuPopupOpen by remember { mutableStateOf(false) }
    var isSslDetailOpen by remember { mutableStateOf(false) }

    // Saved inputs
    val pendingPasswordSave by viewModel.pendingPasswordSave.collectAsState()
    val readerModeActive by viewModel.readerModeActive.collectAsState()
    val readerContentHtml by viewModel.readerContentHtml.collectAsState()
    val capturedBitmap by viewModel.capturedBitmap.collectAsState()
    val securityReport by viewModel.securityReport.collectAsState()
    val isSecurityChecking by viewModel.isSecurityChecking.collectAsState()

    var activeWebViewReference by remember { mutableStateOf<WebView?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    // Launcher for voice input recognizer
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val spokenText = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spokenText.isNullOrBlank()) {
            viewModel.setInputUrl(spokenText)
            viewModel.formatAndLoadUrl(spokenText)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Standard action bar if screen is in regular browser state
            if (currentScreen is BrowserScreen.Browser && !readerModeActive && capturedBitmap == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 0.dp,
                    color = if (activeTab?.isIncognito == true) Color(0xFF1D0E38) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        if (activeTab?.isIncognito != true) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { activeWebViewReference?.goBack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = if (activeTab?.isIncognito == true) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = { activeWebViewReference?.goForward() }) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Forward", tint = if (activeTab?.isIncognito == true) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = { activeWebViewReference?.reload() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = if (activeTab?.isIncognito == true) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = { viewModel.loadUrl("https://www.google.com") }) {
                                Icon(Icons.Default.Home, contentDescription = "Home", tint = if (activeTab?.isIncognito == true) Color.White else MaterialTheme.colorScheme.onSurface)
                            }

                            // AI co-pilot widget switcher
                            IconButton(onClick = { isAiPanelOpen = !isAiPanelOpen }) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = "AI Assistant",
                                    tint = if (isAiPanelOpen) MaterialTheme.colorScheme.primary else (if (activeTab?.isIncognito == true) Color.White else MaterialTheme.colorScheme.onSurface)
                                )
                            }

                            // Tab Count indicator badge
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .then(
                                        if (activeTab?.isIncognito == true) {
                                            Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF381F6D))
                                        } else {
                                            Modifier
                                                .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(6.dp))
                                        }
                                    )
                                    .clickable { isTabSelectorOpen = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${viewModel.tabs.value.size}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeTab?.isIncognito == true) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }

                            // More action menu popup trigger
                            IconButton(onClick = { isMenuPopupOpen = !isMenuPopupOpen }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = if (activeTab?.isIncognito == true) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Native system back interceptor mapping for Chrome-like fidelity
            val backEnabled = isTabSelectorOpen || isAiPanelOpen || isMenuPopupOpen || isSslDetailOpen || 
                              currentScreen !is BrowserScreen.Browser || readerModeActive || 
                              capturedBitmap != null || (activeWebViewReference?.canGoBack() == true)

            BackHandler(enabled = backEnabled) {
                when {
                    isTabSelectorOpen -> isTabSelectorOpen = false
                    isAiPanelOpen -> isAiPanelOpen = false
                    isMenuPopupOpen -> isMenuPopupOpen = false
                    isSslDetailOpen -> isSslDetailOpen = false
                    currentScreen !is BrowserScreen.Browser -> viewModel.setScreen(BrowserScreen.Browser)
                    readerModeActive -> viewModel.closeReaderMode()
                    capturedBitmap != null -> viewModel.clearCapturedBitmap()
                    activeWebViewReference?.canGoBack() == true -> activeWebViewReference?.goBack()
                }
            }

            // Core Browser Layout is ALWAYS kept alive to prevent performance-killing reloads
            Column(modifier = Modifier.fillMaxSize()) {
                // Edge-to-edge Top Address Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 0.dp,
                    color = if (activeTab?.isIncognito == true) Color(0xFF14072E) else MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.statusBarsPadding().padding(vertical = 8.dp, horizontal = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Security padlock / SSL Badge
                            IconButton(onClick = { isSslDetailOpen = true }) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Security Status",
                                    tint = if (isSecureHttps) Color(0xFF388E3C) else Color(0xFFD32F2F)
                                )
                            }

                            // Address Bar text-field
                            OutlinedTextField(
                                value = inputUrl,
                                onValueChange = { viewModel.setInputUrl(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Search or type URL", fontSize = 14.sp) },
                                singleLine = true,
                                leadingIcon = {
                                    Row(Modifier.padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (activeTab?.isIncognito == true) {
                                            Icon(Icons.Default.VisibilityOff, contentDescription = "Incognito Mode", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                },
                                trailingIcon = {
                                    Row {
                                        // Speak input search action
                                        IconButton(onClick = {
                                            val spokenIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Search website URL or keyword...")
                                            }
                                            speechLauncher.launch(spokenIntent)
                                        }) {
                                            Icon(Icons.Default.Mic, contentDescription = "Speech Input")
                                        }
                                        // Dynamic single-touch bookmarking icon
                                        var isBookmarkedState by remember { mutableStateOf(false) }
                                        LaunchedEffect(loadedUrl) {
                                            isBookmarkedState = viewModel.isCurrentPageBookmarked()
                                        }
                                        IconButton(onClick = {
                                            viewModel.toggleBookmark(pageTitle, loadedUrl)
                                            isBookmarkedState = !isBookmarkedState
                                        }) {
                                            Icon(
                                                imageVector = if (isBookmarkedState) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = "Bookmark Page",
                                                tint = if (isBookmarkedState) Color(0xFFFFB300) else (if (activeTab?.isIncognito == true) Color.White else MaterialTheme.colorScheme.onSurface)
                                            )
                                        }
                                    }
                                },
                                textStyle = MaterialTheme.typography.bodyMedium,
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        viewModel.formatAndLoadUrl(inputUrl)
                                        keyboardController?.hide()
                                    }
                                ),
                                shape = RoundedCornerShape(26.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (activeTab?.isIncognito == true) Color(0xFFFFB4AB) else MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Transparent,
                                    unfocusedContainerColor = if (activeTab?.isIncognito == true) Color(0xFF2C1E4E) else MaterialTheme.colorScheme.surfaceVariant,
                                    focusedContainerColor = if (activeTab?.isIncognito == true) Color(0xFF2C1E4E) else MaterialTheme.colorScheme.surfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // Profiles indicator pill
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (activeTab?.isIncognito == true) Color(0xFF381F6D) else MaterialTheme.colorScheme.secondary)
                                    .clickable { viewModel.setScreen(BrowserScreen.Settings) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profiles.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeTab?.isIncognito == true) Color.White else MaterialTheme.colorScheme.onSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Linear loading progress indicator matching load status
                if (isWebLoading) {
                    LinearProgressIndicator(
                        progress = webProgress / 100f,
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = if (activeTab?.isIncognito == true) Color(0xFFFFB4AB) else MaterialTheme.colorScheme.primary
                    )
                }

                // Web View Port Frame
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    BrowserWebView(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                        onWebViewCreated = { activeWebViewReference = it }
                    )

                    // AI panel floating overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isAiPanelOpen,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        AiPanel(
                            viewModel = viewModel,
                            onClose = { isAiPanelOpen = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(480.dp)
                                .clip(RoundedCornerShape(18.dp, 18.dp, 0.dp, 0.dp))
                        )
                    }
                }
            }

            // Render secondary screens on top as modal overlay (keeps WebView running under it)
            if (currentScreen !is BrowserScreen.Browser) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .clickable(enabled = true, onClick = {}) // block touch event propagation
                ) {
                    when (currentScreen) {
                        is BrowserScreen.History -> HistoryView(viewModel)
                        is BrowserScreen.Bookmarks -> BookmarksView(viewModel)
                        is BrowserScreen.Passwords -> PasswordsView(viewModel)
                        is BrowserScreen.Downloads -> DownloadsView(viewModel)
                        is BrowserScreen.Settings -> SettingsView(viewModel)
                        else -> {}
                    }
                }
            }

            // Tab Selector System Side Drawer
            androidx.compose.animation.AnimatedVisibility(
                visible = isTabSelectorOpen,
                enter = slideInHorizontally(initialOffsetX = { -it }),
                exit = slideOutHorizontally(targetOffsetX = { -it })
            ) {
                TabSelector(
                    viewModel = viewModel,
                    onClose = { isTabSelectorOpen = false }
                )
            }

            // General dropdown settings modal list
            if (isMenuPopupOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { isMenuPopupOpen = false }
                ) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                            .width(220.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            DropdownMenuItem(
                                text = { Text("Browsing History") },
                                onClick = {
                                    isMenuPopupOpen = false
                                    viewModel.setScreen(BrowserScreen.History)
                                },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Bookmarks") },
                                onClick = {
                                    isMenuPopupOpen = false
                                    viewModel.setScreen(BrowserScreen.Bookmarks)
                                },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Password Manager") },
                                onClick = {
                                    isMenuPopupOpen = false
                                    viewModel.setScreen(BrowserScreen.Passwords)
                                },
                                leadingIcon = { Icon(Icons.Default.Password, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Downloads") },
                                onClick = {
                                    isMenuPopupOpen = false
                                    viewModel.setScreen(BrowserScreen.Downloads)
                                },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                            )
                            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                            DropdownMenuItem(
                                text = { Text("Reading Mode") },
                                onClick = {
                                    isMenuPopupOpen = false
                                    viewModel.toggleReaderMode()
                                },
                                leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Take Web Screenshot") },
                                onClick = {
                                    isMenuPopupOpen = false
                                    activeWebViewReference?.let { webView ->
                                        // Take a bitmap screenshot of the current WebView render
                                        val bitmap = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
                                        val canvas = android.graphics.Canvas(bitmap)
                                        webView.draw(canvas)
                                        viewModel.triggerCapturedBitmap(bitmap)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (activeTab?.isIncognito == true) "Incognito Mode: ON" else "Incognito Mode: OFF") },
                                onClick = {
                                    isMenuPopupOpen = false
                                    viewModel.toggleIncognitoMode()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (activeTab?.isIncognito == true) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Incognito Mode",
                                        tint = if (activeTab?.isIncognito == true) Color(0xFFFFB4AB) else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                trailingIcon = {
                                    Switch(
                                        checked = activeTab?.isIncognito == true,
                                        onCheckedChange = { _ ->
                                            isMenuPopupOpen = false
                                            viewModel.toggleIncognitoMode()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFFFFB4AB),
                                            checkedTrackColor = Color(0xFF381F6D)
                                        )
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    isMenuPopupOpen = false
                                    viewModel.setScreen(BrowserScreen.Settings)
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                        }
                    }
                }
            }

            // Reader Mode overlay co-pilot dialog
            if (readerModeActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Comfort Reading Board", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { viewModel.closeReaderMode() }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Reader Mode")
                            }
                        }

                        if (isWebLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = pageTitle,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 28.sp
                                )
                                Text(
                                    text = "Polished cleanly by Gemini reader co-pilot",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = readerContentHtml,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            }

            // Dedicated SSL certificate dialog detail
            if (isSslDetailOpen) {
                AlertDialog(
                    onDismissRequest = { 
                        isSslDetailOpen = false 
                        viewModel.clearSecurityReport()
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isSecureHttps) Icons.Default.Security else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isSecureHttps) Color(0xFF388E3C) else Color(0xFFD32F2F)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Security Certificate Shield", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Page Host: ${loadedUrl}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = if (isSecureHttps) {
                                    "✓ Secure Connection (HTTPS)\n" +
                                            "Certificate is verified. Communications are fully encrypted using latest TLS algorithms, guarding against man-in-the-middle exploits."
                                } else {
                                    "⚠ Unsecured connections (HTTP)\n" +
                                            "Communications is plaintext! Avoid entering any logins, credentials or card data."
                                },
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            if (securityReport == null && !isSecurityChecking) {
                                Button(
                                    onClick = { viewModel.auditWebsiteSecurity() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Analyze Safety with Nexa AI", fontSize = 12.sp)
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("AI Safe Browsing Audit:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        if (isSecurityChecking) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Nexa Cyber AI is auditing safety...", fontSize = 11.sp)
                                            }
                                        } else {
                                            Text(securityReport ?: "", fontSize = 11.sp, lineHeight = 15.sp)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { 
                            isSslDetailOpen = false 
                            viewModel.clearSecurityReport()
                        }) {
                            Text("Dismiss")
                        }
                    }
                )
            }

            // Credential password recorder autofill saver prompt bar
            pendingPasswordSave?.let { saveRecord ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Password, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save credentials to password manager?", fontWeight = FontWeight.Bold)
                            }
                            Text("Website secure domain: ${saveRecord.site}", fontSize = 12.sp)
                            Text("Credentials Username: ${saveRecord.username}", fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = { viewModel.dismissSavePassword() }) {
                                    Text("Never for this site")
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { viewModel.dismissSavePassword() }) {
                                        Text("Cancel")
                                    }
                                    Button(onClick = { viewModel.confirmSavePassword() }) {
                                        Text("Save")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Drawing Sketch Annotations overlays
            capturedBitmap?.let { bmp ->
                ScreenshotOverlay(
                    viewModel = viewModel,
                    bitmap = bmp,
                    onClose = { viewModel.clearCapturedBitmap() },
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                )
            }
        }
    }
}
