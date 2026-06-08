package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

class BrowserWebAppInterface(private val onTextExtracted: (String) -> Unit) {
    @JavascriptInterface
    fun processText(text: String) {
        onTextExtracted(text)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    val tab by viewModel.activeTab.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val isIncognito = tab?.isIncognito ?: false

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Fast loading synchronizer for tab switcher
    LaunchedEffect(webViewInstance, activeTabId) {
        val webView = webViewInstance ?: return@LaunchedEffect
        val targetUrl = tab?.url ?: "about:blank"
        
        val currentWebViewUrl = webView.url ?: ""
        val cleanWeb = currentWebViewUrl.trim().removeSuffix("/")
        val cleanTarget = targetUrl.trim().removeSuffix("/")
        
        if (cleanWeb != cleanTarget && targetUrl != "about:blank" && !targetUrl.startsWith("file://")) {
            webView.loadUrl(targetUrl)
        }
    }

    // Fast loading synchronizer for explicit load events
    LaunchedEffect(webViewInstance) {
        val webView = webViewInstance ?: return@LaunchedEffect
        viewModel.navigationEvents.collect { url ->
            if (url != "about:blank" && !url.startsWith("file://")) {
                if (webView.url != url) {
                    webView.loadUrl(url)
                }
            }
        }
    }

    // Configure cookies and cache once when incognito status changes
    LaunchedEffect(webViewInstance, isIncognito) {
        val webView = webViewInstance ?: return@LaunchedEffect
        val cookieManager = CookieManager.getInstance()
        if (isIncognito) {
            cookieManager.setAcceptCookie(false)
            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            webView.clearCache(true)
        } else {
            cookieManager.setAcceptCookie(true)
            webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        }
    }

    // Listen to explicit private session clearing events (e.g., when a private tab is closed or incognito is toggled off)
    LaunchedEffect(webViewInstance) {
        val webView = webViewInstance ?: return@LaunchedEffect
        viewModel.clearSessionEvents.collect {
            webView.clearHistory()
            webView.clearCache(true)
            webView.clearSslPreferences()
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeSessionCookies(null)
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    allowFileAccess = true
                    allowContentAccess = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    databaseEnabled = true
                }

                addJavascriptInterface(BrowserWebAppInterface { text ->
                    viewModel.extractedPageContent = text
                }, "AndroidApp")

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { viewModel.onPageStarted(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val title = view?.title ?: ""
                        url?.let { viewModel.onPageFinished(it, title) }
                        
                        // Extract HTML clean text content for the Gemini AI contexts
                        view?.evaluateJavascript(
                            "javascript:(function() { " +
                                    "var text = document.body.innerText || ''; " +
                                    "window.AndroidApp.processText(text);" +
                                    "})();",
                            null
                        )

                        // Attempt password manager checks
                        url?.let {
                            viewModel.checkAndAutofillPasswords(it) { credentials ->
                                // Optional autofill: inject JS values into standard input text types
                                view?.evaluateJavascript(
                                    "javascript:(function() { " +
                                             "var inputs = document.getElementsByTagName('input');" +
                                             "for(var i=0; i<inputs.length; i++) {" +
                                             "   if(inputs[i].type === 'text' || inputs[i].type === 'email') {" +
                                             "       inputs[i].value = '${credentials.username}';" +
                                             "   }" +
                                             "   if(inputs[i].type === 'password') {" +
                                             "       inputs[i].value = '${credentials.password}';" +
                                             "   }" +
                                             "}" +
                                             "})();",
                                    null
                                )
                            }
                        }
                    }

                    // --- Ad Blocker Implementation ---
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val adBlockEnabled = viewModel.adBlockerEnabled.value
                        if (adBlockEnabled && request != null) {
                            val requestUrl = request.url.toString()
                            val adKeywords = listOf(
                                "googleadservices", "pagead", "doubleclick",
                                "adservice", "adnxs", "adsystem", "scorecardresearch",
                                "quantserve", "amazon-adsystem", "pubads", "googlesyndication"
                            )
                            for (keyword in adKeywords) {
                                if (requestUrl.contains(keyword)) {
                                    // Block the ad asset request by returning an empty input stream
                                    return WebResourceResponse(
                                        "text/javascript",
                                        "UTF-8",
                                        ByteArrayInputStream("".toByteArray())
                                    )
                                }
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        viewModel.onLoadingProgress(newProgress)
                    }

                    // --- Popup Blocker Implementation ---
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: android.os.Message?
                    ): Boolean {
                        val popupBlockEnabled = viewModel.popupBlockerEnabled.value
                        if (popupBlockEnabled) {
                            // Block unsolicited window creations/popups
                            return false
                        }
                        return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
                    }
                }

                setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                    // Extract fileName
                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                    viewModel.registerDownload(fileName, url)
                }

                webViewInstance = this
                onWebViewCreated(this)
            }
        },
        update = {
            // Handled efficiently via LaunchedEffects
        },
        modifier = modifier
    )
}
