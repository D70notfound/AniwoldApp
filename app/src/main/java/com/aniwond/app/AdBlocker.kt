package com.aniwond.app

import android.content.Context
import android.net.Uri

object AdBlocker {

    private val blockedDomains = HashSet<String>()

    fun init(context: Context) {
        try {
            context.assets.open("adblock_domains.txt").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        blockedDomains.add(trimmed.lowercase())
                    }
                }
            }
        } catch (e: Exception) {
            // If asset file fails to load, proceed with empty set
        }
    }

    fun isBlocked(url: String): Boolean {
        return try {
            val host = Uri.parse(url).host?.lowercase() ?: return false
            isHostBlocked(host)
        } catch (e: Exception) {
            false
        }
    }

    private fun isHostBlocked(host: String): Boolean {
        // Check exact match and parent domains
        var domain = host
        while (domain.isNotEmpty()) {
            if (blockedDomains.contains(domain)) return true
            val dotIndex = domain.indexOf('.')
            if (dotIndex == -1) break
            domain = domain.substring(dotIndex + 1)
        }
        return false
    }
}
