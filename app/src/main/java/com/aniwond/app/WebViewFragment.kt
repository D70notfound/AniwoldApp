package com.aniwond.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray

class WebViewFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var hiddenWebView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabDownload: FloatingActionButton

    private var currentEpisodeMatch: MatchResult? = null
    private val downloadQueueManager by lazy { DownloadQueueManager(requireContext()) }

    private val episodePattern = Regex(
        """/anime/stream/([^/]+)/staffel-(\d+)/episode-(\d+)"""
    )

    companion object {
        private const val HOME_URL = "https://aniworld.to"
        private const val STATE_URL = "state_url"
    }

    // Runtime permission launcher (API 23–28 only)
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                requireContext(),
                getString(R.string.permission_storage_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // JS interface for the main WebView — stores found video URL
    private inner class MainBridge {
        @JavascriptInterface
        fun onVideoFound(url: String) {
            // stored in window._dlVideoUrl by JS — this interface is a backup trigger
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_webview, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar   = view.findViewById(R.id.progressBar)
        webView       = view.findViewById(R.id.webView)
        hiddenWebView = view.findViewById(R.id.hiddenWebView)
        fabDownload   = view.findViewById(R.id.fab_download)

        AdBlocker.init(requireContext())

        configureWebView(webView)
        configureWebView(hiddenWebView)

        // Main WebView clients
        val mainClient = AniWebViewClient(progressBar) { url -> onPageUrlChanged(url) }
        webView.webViewClient   = mainClient
        webView.webChromeClient = AniWebChromeClient(progressBar)
        webView.setDownloadListener { url, _, _, _, _ -> triggerDownload(url) }

        // Hidden WebView uses same client but no progress bar / FAB callback
        hiddenWebView.webViewClient = AniWebViewClient(ProgressBar(requireContext()))
        hiddenWebView.webChromeClient = AniWebChromeClient(ProgressBar(requireContext()))

        fabDownload.setOnClickListener { showDownloadDialog() }

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(HOME_URL)
        }
    }

    private fun configureWebView(wv: WebView) {
        val s: WebSettings = wv.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.builtInZoomControls = false
        s.displayZoomControls = false
        s.setSupportMultipleWindows(false)
        s.javaScriptCanOpenWindowsAutomatically = false
        s.mediaPlaybackRequiresUserGesture = false
        s.allowContentAccess = true
        s.allowFileAccess = false
        s.setSupportZoom(true)
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    private fun onPageUrlChanged(url: String) {
        val match = episodePattern.find(url)
        currentEpisodeMatch = match
        fabDownload.isVisible = match != null
    }

    private fun showDownloadDialog() {
        val match = currentEpisodeMatch ?: return
        val staffelNr = match.groupValues[2]

        DownloadChoiceBottomSheet(
            staffelNr = staffelNr,
            onEpisode = { downloadCurrentEpisode() },
            onSeason  = { downloadSeason(match.groupValues[1], staffelNr) }
        ).show(childFragmentManager, "download_choice")
    }

    private fun downloadCurrentEpisode() {
        ensureStoragePermission {
            webView.evaluateJavascript("window._dlVideoUrl || null") { result ->
                val url = parseJsString(result)
                if (!url.isNullOrBlank()) {
                    VideoDownloader.download(requireContext(), url)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.download_no_video_found),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun downloadSeason(animeName: String, staffelNr: String) {
        Toast.makeText(requireContext(), getString(R.string.download_fetching_episodes), Toast.LENGTH_SHORT).show()

        // Extract all episode URLs from the current page via JS
        val js = """
            (function() {
                var pattern = '/staffel-$staffelNr/episode-';
                var seen = {};
                var result = [];
                document.querySelectorAll('a[href*="' + pattern + '"]').forEach(function(a) {
                    var h = a.href;
                    if (h && !seen[h]) { seen[h] = true; result.push(h); }
                });
                result.sort(function(a, b) {
                    var na = parseInt(a.replace(/.*episode-/, '') || '0');
                    var nb = parseInt(b.replace(/.*episode-/, '') || '0');
                    return na - nb;
                });
                return JSON.stringify(result);
            })()
        """.trimIndent()

        webView.evaluateJavascript(js) { jsonResult ->
            val urls = parseJsJsonArray(jsonResult)
            if (urls.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.download_no_episodes_found),
                    Toast.LENGTH_LONG
                ).show()
                return@evaluateJavascript
            }
            ensureStoragePermission {
                downloadQueueManager.start(urls, hiddenWebView)
            }
        }
    }

    private fun triggerDownload(url: String) {
        ensureStoragePermission { VideoDownloader.download(requireContext(), url) }
    }

    private fun ensureStoragePermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            action()
        }
    }

    /** Parse the return value of evaluateJavascript for a string. */
    private fun parseJsString(raw: String?): String? {
        if (raw == null || raw == "null") return null
        return if (raw.startsWith("\"") && raw.endsWith("\"")) {
            raw.substring(1, raw.length - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/", "/")
                .takeIf { it.isNotBlank() }
        } else raw.takeIf { it.isNotBlank() && it != "null" }
    }

    /** Parse the return value of evaluateJavascript for a JSON array of strings. */
    private fun parseJsJsonArray(raw: String?): List<String> {
        if (raw == null || raw == "null") return emptyList()
        return try {
            val unescaped = if (raw.startsWith("\"") && raw.endsWith("\"")) {
                raw.substring(1, raw.length - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\/", "/")
            } else raw
            val arr = JSONArray(unescaped)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun onBackPressed(): Boolean {
        if (webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::webView.isInitialized) webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.onResume()
            webView.resumeTimers()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::webView.isInitialized) {
            webView.onPause()
            webView.pauseTimers()
        }
    }

    override fun onDestroyView() {
        downloadQueueManager.cancel()
        if (::webView.isInitialized) webView.destroy()
        if (::hiddenWebView.isInitialized) hiddenWebView.destroy()
        super.onDestroyView()
    }
}
