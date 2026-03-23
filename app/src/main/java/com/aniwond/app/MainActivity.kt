package com.aniwond.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private var webViewFragment: WebViewFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)

        if (savedInstanceState == null) {
            webViewFragment = WebViewFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, webViewFragment!!, "webview")
                .commit()
        } else {
            webViewFragment = supportFragmentManager
                .findFragmentByTag("webview") as? WebViewFragment
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_browser -> {
                    val frag = webViewFragment ?: WebViewFragment().also { webViewFragment = it }
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, frag, "webview")
                        .commit()
                    true
                }
                R.id.nav_downloads -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, DownloadsFragment(), "downloads")
                        .commit()
                    true
                }
                else -> false
            }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        val wvFrag = supportFragmentManager.findFragmentByTag("webview") as? WebViewFragment
        if (wvFrag != null && wvFrag.isVisible && wvFrag.onBackPressed()) return
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}
