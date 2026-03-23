package com.aniwond.app

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.core.view.isVisible

class AniWebViewClient(
    private val progressBar: ProgressBar,
    private val onUrlChanged: ((String) -> Unit)? = null
) : WebViewClient() {

    companion object {
        // Domains allowed to load in the main frame (e.g. video players)
        private val ALLOWED_NAVIGATE_HOSTS = setOf(
            "aniworld.to",
            "www.aniworld.to",
            "voe.sx",
            "www.voe.sx",
            "streamtape.com",
            "www.streamtape.com",
            "doodstream.com",
            "www.doodstream.com",
            "vidoza.net",
            "www.vidoza.net",
            "filemoon.sx",
            "www.filemoon.sx",
            "vidmoly.to",
            "www.vidmoly.to",
            "mixdrop.co",
            "www.mixdrop.co",
            "upstream.to",
            "www.upstream.to",
            "speedfiles.net",
            "www.speedfiles.net",
            "vidlox.me",
            "www.vidlox.me"
        )

        // CSS injected after page load to remove ad container elements
        private val AD_HIDE_CSS = """
            .banner, .ad, .ads, .advertisement, .ad-container, .ad-wrapper,
            [class*="ad-"], [class*="-ad"], [id*="ad-"], [id*="-ad"],
            [class*="banner"], [id*="banner"],
            iframe[src*="ads"], iframe[src*="doubleclick"],
            .popup, .overlay:not(.video-overlay), .modal-backdrop.ad {
                display: none !important;
                visibility: hidden !important;
                height: 0 !important;
                width: 0 !important;
                overflow: hidden !important;
            }
        """.trimIndent()
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val url = request.url.toString()
        if (AdBlocker.isBlocked(url)) {
            return WebResourceResponse("text/plain", "utf-8", null)
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val host = request.url.host?.lowercase() ?: return true
        return if (isAllowedHost(host)) {
            false // Allow WebView to load it
        } else {
            true  // Block external redirect
        }
    }

    private fun isAllowedHost(host: String): Boolean {
        return ALLOWED_NAVIGATE_HOSTS.any { allowed ->
            host == allowed || host.endsWith(".$allowed")
        }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        progressBar.isVisible = true
        onUrlChanged?.invoke(url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        progressBar.isVisible = false
        onUrlChanged?.invoke(url)
        injectAdHideCSS(view)
        injectPopupBlockerJS(view)
        injectVideoDetectorJS(view)
    }

    private fun injectAdHideCSS(view: WebView) {
        val escaped = AD_HIDE_CSS.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "")
        val js = """
            (function() {
                var style = document.createElement('style');
                style.type = 'text/css';
                style.appendChild(document.createTextNode("$escaped"));
                document.head.appendChild(style);
            })();
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }

    private fun injectPopupBlockerJS(view: WebView) {
        val js = """
            (function() {
                // Block window.open popups
                window.open = function() { return null; };

                // Rewrite _blank links to stay in same window
                document.addEventListener('click', function(e) {
                    var el = e.target;
                    while (el && el.tagName !== 'A') {
                        el = el.parentElement;
                    }
                    if (el && el.target === '_blank') {
                        el.target = '_self';
                    }
                }, true);

                // Remove existing _blank attributes
                document.querySelectorAll('a[target="_blank"]').forEach(function(a) {
                    a.target = '_self';
                });
            })();
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }

    /**
     * Scans for <video> elements and stores the first direct video URL in
     * window._dlVideoUrl so the fragment can retrieve it via evaluateJavascript.
     */
    private fun injectVideoDetectorJS(view: WebView) {
        val js = """
            (function() {
                if (typeof window._dlObserver === 'undefined') {
                    window._dlVideoUrl = window._dlVideoUrl || null;

                    function captureVideo(v) {
                        var src = v.currentSrc || v.src ||
                                  (v.querySelector('source') ? v.querySelector('source').src : null);
                        if (src && src.length > 0 &&
                            !src.startsWith('blob:') && !src.startsWith('data:')) {
                            window._dlVideoUrl = src;
                        }
                    }

                    document.querySelectorAll('video').forEach(captureVideo);

                    window._dlObserver = new MutationObserver(function() {
                        document.querySelectorAll('video').forEach(captureVideo);
                    });
                    window._dlObserver.observe(document.documentElement,
                        { childList: true, subtree: true, attributes: true, attributeFilter: ['src'] });

                    document.querySelectorAll('video').forEach(function(v) {
                        v.addEventListener('loadedmetadata', function() { captureVideo(v); });
                        v.addEventListener('play', function() { captureVideo(v); });
                    });
                }
            })();
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }
}
