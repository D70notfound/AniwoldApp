package com.aniwond.app

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

object VideoDownloader {

    fun download(context: Context, videoUrl: String) {
        if (videoUrl.isBlank()) return

        if (videoUrl.contains(".m3u8")) {
            Toast.makeText(
                context,
                context.getString(R.string.download_hls_unsupported),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val filename = extractFilename(videoUrl)
        val request = DownloadManager.Request(Uri.parse(videoUrl)).apply {
            setTitle(filename)
            setDescription("AniwoldApp")
            setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "AniwoldApp/$filename"
            )
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
            addRequestHeader(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
            )
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, context.getString(R.string.download_started), Toast.LENGTH_SHORT).show()
    }

    private fun extractFilename(url: String): String {
        return try {
            val lastSegment = Uri.parse(url).lastPathSegment
            if (!lastSegment.isNullOrBlank() && lastSegment.contains(".")) {
                lastSegment
            } else {
                "video_${System.currentTimeMillis()}.mp4"
            }
        } catch (e: Exception) {
            "video_${System.currentTimeMillis()}.mp4"
        }
    }
}
