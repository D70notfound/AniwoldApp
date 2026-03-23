package com.aniwond.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.widget.Toast

/**
 * Sequentially loads episode pages in a background WebView, waits for a video URL
 * to be detected via JavaScript, and then triggers a download before moving to the
 * next episode.
 */
class DownloadQueueManager(private val context: Context) {

    private val queue = ArrayDeque<String>()
    private val handler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var pollCount = 0
    private var cancelled = false

    private val pollIntervalMs = 500L
    private val maxPolls = 30  // 15 seconds max per episode

    fun start(episodeUrls: List<String>, hiddenWebView: WebView) {
        cancelled = false
        queue.clear()
        queue.addAll(episodeUrls)
        webView = hiddenWebView
        processNext()
    }

    fun cancel() {
        cancelled = true
        handler.removeCallbacksAndMessages(null)
        queue.clear()
    }

    private fun processNext() {
        if (cancelled) return
        val url = queue.removeFirstOrNull() ?: run {
            Toast.makeText(
                context,
                context.getString(R.string.download_season_complete),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        pollCount = 0
        webView?.loadUrl(url)
        // Give the page time to start loading before first poll
        handler.postDelayed({ pollForVideo() }, 2000L)
    }

    private fun pollForVideo() {
        if (cancelled) return
        val wv = webView ?: return

        wv.evaluateJavascript("window._dlVideoUrl || null") { result ->
            if (cancelled) return@evaluateJavascript

            val videoUrl = parseJsString(result)
            if (!videoUrl.isNullOrBlank()) {
                // Found a video — download it and clear the JS variable
                VideoDownloader.download(context, videoUrl)
                wv.evaluateJavascript("window._dlVideoUrl = null", null)
                // Wait a moment then load next episode
                handler.postDelayed({ processNext() }, 1500L)
            } else if (pollCount < maxPolls) {
                pollCount++
                handler.postDelayed({ pollForVideo() }, pollIntervalMs)
            } else {
                // Timeout — skip this episode
                processNext()
            }
        }
    }

    /** Unwrap the JS evaluateJavascript result (which is JSON-encoded). */
    private fun parseJsString(raw: String?): String? {
        if (raw == null || raw == "null") return null
        // evaluateJavascript returns strings wrapped in quotes with escaped content
        return if (raw.startsWith("\"") && raw.endsWith("\"")) {
            raw.substring(1, raw.length - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/", "/")
                .takeIf { it.isNotBlank() }
        } else raw.takeIf { it.isNotBlank() && it != "null" }
    }
}
