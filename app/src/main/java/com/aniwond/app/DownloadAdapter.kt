package com.aniwond.app

import android.app.DownloadManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class DownloadItem(
    val id: Long,
    val title: String,
    val status: Int,
    val totalBytes: Long,
    val downloadedBytes: Long
)

class DownloadAdapter(
    private val items: List<DownloadItem>,
    private val onClick: (DownloadItem) -> Unit
) : RecyclerView.Adapter<DownloadAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivStatus: ImageView = view.findViewById(R.id.ivStatus)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_download, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvStatus.text = statusLabel(holder.itemView, item)
        holder.itemView.setOnClickListener {
            if (item.status == DownloadManager.STATUS_SUCCESSFUL) onClick(item)
        }
    }

    override fun getItemCount() = items.size

    private fun statusLabel(view: View, item: DownloadItem): String {
        val ctx = view.context
        return when (item.status) {
            DownloadManager.STATUS_RUNNING -> {
                val percent = if (item.totalBytes > 0) {
                    (item.downloadedBytes * 100 / item.totalBytes).toInt()
                } else 0
                "${ctx.getString(R.string.download_status_running)} $percent%"
            }
            DownloadManager.STATUS_SUCCESSFUL -> ctx.getString(R.string.download_status_done)
            DownloadManager.STATUS_FAILED    -> ctx.getString(R.string.download_status_failed)
            else                             -> ctx.getString(R.string.download_status_pending)
        }
    }
}
