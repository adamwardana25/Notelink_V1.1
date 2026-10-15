package com.example.notelink.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Activity
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Link
import com.example.notelink.data.model.Link
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    url: String, 
    allLinks: List<Link> = emptyList(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val trimmedUrl = remember(url) { url.trim() }
    
    // Tab Management
    val openedUrls = remember { mutableStateListOf<String>() }
    val tabLoadingStates = remember { mutableStateMapOf<String, Boolean>() }
    val tabProgressStates = remember { mutableStateMapOf<String, Float>() }

    LaunchedEffect(trimmedUrl) {
        if (trimmedUrl !in openedUrls) {
            openedUrls.add(trimmedUrl)
        }
    }

    var showLinkSwitcher by remember { mutableStateOf(false) }

    // Handle Immersive Mode (Removed hide to prevent overlapping)
    SideEffect {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            // controller.hide(WindowInsetsCompat.Type.systemBars()) // Kita tampilkan lagi agar tidak terhalang
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Restore System Bars on Dispose
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    var pendingDownload by remember { mutableStateOf<DownloadData?>(null) }
    var mFilePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val singleFilePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        mFilePathCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
        mFilePathCallback = null
    }

    val multiFilePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        mFilePathCallback?.onReceiveValue(if (uris.isNotEmpty()) uris.toTypedArray() else null)
        mFilePathCallback = null
    }

    // Permission handling for downloads
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && pendingDownload != null) {
            val data = pendingDownload!!
            handleDownload(data.context, data.url, data.userAgent, data.contentDisposition, data.mimetype)
            pendingDownload = null
        } else if (!isGranted) {
            Toast.makeText(context, "Storage permission is required to download files.", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkAndDownload(data: DownloadData) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            handleDownload(data.context, data.url, data.userAgent, data.contentDisposition, data.mimetype)
        } else {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                handleDownload(data.context, data.url, data.userAgent, data.contentDisposition, data.mimetype)
            } else {
                pendingDownload = data
                permissionLauncher.launch(permission)
            }
        }
    }

    // Handle back button for WebView navigation
    // Tambahkan LaunchedEffect untuk memastikan webViewInstance sudah siap sebelum mengizinkan BackHandler
    var isReady by remember { mutableStateOf(false) }
    LaunchedEffect(webViewInstance) {
        if (webViewInstance != null) isReady = true
    }

    BackHandler(enabled = isReady) {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onBack()
        }
    }

    // Root Box dengan background hitam
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        openedUrls.forEach { tabUrl ->
            val isVisible = tabUrl == trimmedUrl
            val tabIsGemini = tabUrl.contains("gemini.google")
            val tabIsAiStudio = tabUrl.contains("aistudio.google") || tabUrl.contains("ai.studio")

            key(tabUrl) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            setLayerType(View.LAYER_TYPE_HARDWARE, null)
                            setBackgroundColor(android.graphics.Color.BLACK)
                            WebView.setWebContentsDebuggingEnabled(true)

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    tabLoadingStates[tabUrl] = true
                                    view?.setBackgroundColor(android.graphics.Color.BLACK)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    tabLoadingStates[tabUrl] = false
                                    if (tabIsGemini) injectGeminiCanvasCleaner(view)
                                    if (tabIsAiStudio) injectAiStudioCleaner(view)
                                }

                                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                    if (request?.isForMainFrame == true) {
                                        Log.e("NotelinkBrowser", "Error: ${error?.description}")
                                    }
                                }

                                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                                    handler?.proceed()
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    tabProgressStates[tabUrl] = newProgress / 100f
                                    if (newProgress > 50) {
                                        if (tabIsGemini) injectGeminiCanvasCleaner(view)
                                        if (tabIsAiStudio) injectAiStudioCleaner(view)
                                    }
                                }

                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    filePathCallback: ValueCallback<Array<Uri>>?,
                                    fileChooserParams: FileChooserParams?
                                ): Boolean {
                                    mFilePathCallback?.onReceiveValue(null)
                                    mFilePathCallback = filePathCallback
                                    
                                    val mimeTypes = fileChooserParams?.acceptTypes?.filter { it.isNotBlank() }
                                    val mimeType = if (mimeTypes.isNullOrEmpty()) "*/*" else mimeTypes[0]
                                    val isMultiple = fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                                    
                                    try {
                                        if (isMultiple) {
                                            multiFilePickerLauncher.launch(mimeType)
                                        } else {
                                            singleFilePickerLauncher.launch(mimeType)
                                        }
                                    } catch (_: Exception) {
                                        mFilePathCallback?.onReceiveValue(null)
                                        mFilePathCallback = null
                                        return false
                                    }
                                    return true
                                }
                            }

                            setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, _ ->
                                checkAndDownload(DownloadData(ctx, downloadUrl, userAgent, contentDisposition, mimetype))
                            }

                            setOnLongClickListener {
                                val result = hitTestResult
                                val type = result.type
                                if (type == WebView.HitTestResult.IMAGE_TYPE ||
                                    type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE ||
                                    type == WebView.HitTestResult.IMAGE_ANCHOR_TYPE) {

                                    val imageUrl = result.extra
                                    if (imageUrl != null) {
                                        showImageOptions(ctx, imageUrl, settings.userAgentString) {
                                            checkAndDownload(DownloadData(ctx, imageUrl, settings.userAgentString, null, null))
                                        }
                                    }
                                    true
                                } else {
                                    false
                                }
                            }

                            @SuppressLint("SetJavaScriptEnabled")
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                @Suppress("DEPRECATION")
                                databaseEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                javaScriptCanOpenWindowsAutomatically = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                                userAgentString = if (tabIsAiStudio) {
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                                } else {
                                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                }

                                setAllowFileAccess(true)
                                setAllowContentAccess(true)
                                setSupportMultipleWindows(false)
                            }
                            loadUrl(tabUrl)
                        }
                    },
                    update = { view ->
                        view.visibility = if (isVisible) View.VISIBLE else View.GONE
                        if (isVisible) {
                            webViewInstance = view
                        }
                    }
                )
            }
        }

        val currentIsLoading = tabLoadingStates[trimmedUrl] ?: true
        val currentProgress = tabProgressStates[trimmedUrl] ?: 0f

        if (currentIsLoading) {
            LinearProgressIndicator(
                progress = { currentProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f),
                color = Color(0xFF00E5FF),
                trackColor = Color(0xFF1E293B)
            )
        }

        // Home Button at absolute Bottom Left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .zIndex(2f),
            contentAlignment = Alignment.BottomStart
        ) {
            Surface(
                onClick = onBack,
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                color = Color.Black,
                contentColor = Color.White,
                modifier = Modifier.width(44.dp).height(60.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        modifier = Modifier.size(24.dp).offset(x = (-2).dp)
                    )
                }
            }
        }

        // Link Switcher Button at absolute Bottom Right
        if (allLinks.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .zIndex(2f),
                contentAlignment = Alignment.BottomEnd
            ) {
                Surface(
                    onClick = { showLinkSwitcher = true },
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                    color = Color.Black,
                    contentColor = Color.White,
                    modifier = Modifier.width(44.dp).height(60.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Switch Tab",
                            modifier = Modifier.size(24.dp).offset(x = 2.dp)
                        )
                    }
                }
            }
        }

        // --- CUSTOM TAB SWITCHER OVERLAY (Much smoother than ModalBottomSheet) ---
        AnimatedVisibility(
            visible = showLinkSwitcher,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(10f)
        ) {
            // Background Dim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showLinkSwitcher = false }
            ) {
                // Content Panel
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 24.dp) // Floating margin
                        .clickable(enabled = false) {}
                ) {
                    AnimatedVisibility(
                        visible = showLinkSwitcher,
                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp), // More compact height
                            shape = RoundedCornerShape(28.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.98f), // Glassy look
                            tonalElevation = 6.dp,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                        ) {
                            Column(
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                // Subtle Drag Handle
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(top = 12.dp, bottom = 8.dp)
                                        .size(32.dp, 3.dp)
                                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                )

                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(items = allLinks, key = { it.id }) { link ->
                                        val isSelected = link.url == url
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable {
                                                    showLinkSwitcher = false
                                                    if (!isSelected) onNavigate(link.url)
                                                }
                                                .background(
                                                    if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.08f)
                                                    else Color.Transparent
                                                )
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                // Active Indicator Bar
                                                Box(
                                                    modifier = Modifier
                                                        .width(3.dp)
                                                        .height(20.dp)
                                                        .background(
                                                            if (isSelected) Color(0xFF00E5FF) else Color.Transparent,
                                                            CircleShape
                                                        )
                                                )
                                                
                                                Spacer(Modifier.width(12.dp))

                                                // App Icon (Smaller & Simpler)
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White.copy(alpha = 0.05f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val icon = when {
                                                        link.url.contains("gemini") -> Icons.Default.AutoAwesome
                                                        link.url.contains("ai.studio") -> Icons.Default.Code
                                                        else -> Icons.Default.Link
                                                    }
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Spacer(Modifier.width(12.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = link.name,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                        ),
                                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White
                                                    )
                                                    Text(
                                                        text = link.category.uppercase(),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            letterSpacing = 0.5.sp
                                                        ),
                                                        color = Color.White.copy(alpha = 0.3f)
                                                    )
                                                }
                                                
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color(0xFF00E5FF),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cleaner script specifically for Google AI Studio.
 */
private fun injectAiStudioCleaner(view: WebView?) {
    val script = """
        (function() {
            if (window.__aiStudioCleanerInjected) return;
            window.__aiStudioCleanerInjected = true;
            
            function cleanUI() {
              try {
                // Sangat spesifik untuk footer safety info agar tidak blank putih
                var selectors = [
                  '.safety-info-container',
                  '.safety-info-container.ng-star-inserted'
                ];
                
                selectors.forEach(function(s) {
                  var els = document.querySelectorAll(s);
                  els.forEach(function(el) {
                    if (el.innerText && el.innerText.includes('developed by another user')) {
                        el.style.setProperty('display', 'none', 'important');
                        el.style.setProperty('visibility', 'hidden', 'important');
                        el.style.setProperty('height', '0px', 'important');
                        el.style.setProperty('margin', '0px', 'important');
                        el.style.setProperty('padding', '0px', 'important');
                        el.style.setProperty('pointer-events', 'none', 'important');
                    }
                  });
                });
              } catch(e) {}
            }

            cleanUI();
            if (window.MutationObserver) {
                new MutationObserver(cleanUI).observe(document.documentElement, { childList: true, subtree: true });
            }
            // Interval lambat (1 detik) agar tidak membebani loading
            setInterval(cleanUI, 1000);
        })();
    """.trimIndent()
    view?.evaluateJavascript(script, null)
}

/**
 * Robust script to clean Gemini UI and force Fullscreen.
 * Adapted from standard web cleaning patterns.
 */
private fun injectGeminiCanvasCleaner(view: WebView?) {
    val script = """
        (function() {
            if (window.__geminiCleanerInjected) return;
            window.__geminiCleanerInjected = true;

            function cleanUI() {
              try {
                // Targeted selectors to hide header, footer, promos and branding
                var selectors = [
                  'header', 'footer', 'nav', 'aside', 'bard-mode-switcher',
                  '.footer-mobile', '.disclaimer-panel', '[data-test-id="disclaimer"]',
                  '.gb_Td', '.gb_Sd', '.gb_Zd', '.gb_Vd', '.side-nav-menu-button',
                  '[data-test-id="gemini-spark-header"]', '[data-test-id="bard-logo-only"]',
                  '.one-google-bar', '.top-bar', '.header-content', 'chat-window-header',
                  '.app-bar', '.navigation-drawer', '.bottom-bar', '.legal-container',
                  '.mat-typography-disclaimer', '.gemini-canvas-footer', '.gemini-canvas-header',
                  'gm-canvas-footer', 'gm-canvas-header', '.gemini-app-header-container',
                  '[data-test-id="copy-canvas-button"]', '.gemini-button', '.disclaimer-text',
                  '.footer.footer-mobile', 'button.mat-mdc-unelevated-button',
                  '[class*="header"]', '[class*="footer"]', '[class*="nav"]', '[class*="disclaimer"]',
                  '[class*="banner"]', '[class*="promo"]', '[class*="upsell"]', '[class*="canvas-banner"]'
                ];
                
                selectors.forEach(function(s) {
                  var els = document.querySelectorAll(s);
                  els.forEach(function(el) {
                    if (el.id === 'yDmH0d' || el.tagName === 'BODY' || el.tagName === 'HTML') return;
                    el.style.setProperty('display', 'none', 'important');
                    el.style.setProperty('visibility', 'hidden', 'important');
                    el.style.setProperty('height', '0px', 'important');
                    el.style.setProperty('opacity', '0', 'important');
                    el.style.setProperty('pointer-events', 'none', 'important');
                  });
                });

                // Hapus elemen spesifik berdasarkan teks
                var buttons = document.querySelectorAll('button, span, div');
                buttons.forEach(function(el) {
                   if (el.innerText && (el.innerText.includes('Gemini Canvas') || el.innerText.includes('Report unsafe content'))) {
                      var target = el.closest('.footer') || el.closest('button') || el;
                      target.style.setProperty('display', 'none', 'important');
                   }
                });

                // Ensure main container fills screen without gap
                var mainSelectors = ['main', 'chat-app', '.main-content', 'mat-sidenav-content', '.container', '.preview-container', 'web-preview', '.code-content', 'iframe', '.fluid-container'];
                mainSelectors.forEach(function(s) {
                  var els = document.querySelectorAll(s);
                  els.forEach(function(el) {
                    el.style.setProperty('margin', '0px', 'important');
                    el.style.setProperty('padding', '0px', 'important');
                    el.style.setProperty('top', '0px', 'important');
                    el.style.setProperty('width', '100%', 'important');
                    el.style.setProperty('max-width', '100%', 'important');
                    
                    if (el.tagName === 'IFRAME' || s === 'web-preview' || s === '.preview-container') {
                        el.style.setProperty('height', '100vh', 'important');
                        el.style.setProperty('position', 'fixed', 'important');
                        el.style.setProperty('top', '0px', 'important');
                        el.style.setProperty('left', '0px', 'important');
                        el.style.setProperty('z-index', '99999', 'important');
                    }
                  });
                });
                
                document.body.style.margin = '0';
                document.body.style.padding = '0';
                document.documentElement.style.setProperty('--internal-bar-offset', '0px', 'important');

                // Force Dark/Black Background
                var roots = [document.documentElement, document.body];
                roots.forEach(function(el) {
                  if (!el) return;
                  el.style.setProperty('background-color', '#000000', 'important');
                  el.style.setProperty('background', '#000000', 'important');
                  el.style.setProperty('color', '#FFFFFF', 'important');
                  el.style.setProperty('overflow', 'hidden', 'important');
                });
                document.body.style.setProperty('overflow', 'auto', 'important');

                var containers = document.querySelectorAll('.container, main, article, #yDmH0d, .view-content, .chat-window');
                containers.forEach(function(el) {
                  el.style.setProperty('background-color', '#000000', 'important');
                  el.style.setProperty('background', '#000000', 'important');
                });
              } catch(e) {}
            }

            cleanUI();
            if (window.MutationObserver) {
                new MutationObserver(cleanUI).observe(document.documentElement, { childList: true, subtree: true });
            }
            setInterval(cleanUI, 1000);
        })();
    """.trimIndent()
    view?.evaluateJavascript(script, null)
}

// --- Download helpers ---

private data class DownloadData(
    val context: Context, val url: String, val userAgent: String?, val contentDisposition: String?, val mimetype: String?
)

private fun handleDownload(context: Context, url: String, userAgent: String?, contentDisposition: String?, mimetype: String?) {
    if (url.startsWith("data:")) saveDataUrl(context, url)
    else startDownloadManager(context, url, userAgent, contentDisposition, mimetype)
}

private fun saveDataUrl(context: Context, dataUrl: String) {
    try {
        val base64Data = dataUrl.substringAfter(",")
        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        val fileName = "img_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Notelink")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri).use { outputStream ->
                if (outputStream != null) bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            Toast.makeText(context, "Image saved to Gallery", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun startDownloadManager(context: Context, url: String, userAgent: String?, contentDisposition: String?, mimetype: String?) {
    try {
        val uri = Uri.parse(url)
        val request = DownloadManager.Request(uri)
        if (mimetype != null) request.setMimeType(mimetype)
        val cookies = CookieManager.getInstance().getCookie(url)
        request.addRequestHeader("cookie", cookies)
        request.addRequestHeader("User-Agent", userAgent)
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
        request.setTitle(fileName)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun showImageOptions(context: Context, imageUrl: String, userAgent: String?, onDownload: () -> Unit) {
    AlertDialog.Builder(context)
        .setTitle("Image Options")
        .setMessage("Do you want to download this image?")
        .setPositiveButton("Download") { _, _ -> onDownload() }
        .setNegativeButton("Cancel", null)
        .show()
}
