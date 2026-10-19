package com.videodownloader.app.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.videodownloader.app.MainActivity
import com.videodownloader.app.core.DownloadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Foreground service: keeps downloads alive when the app is swiped away, and shows real progress. */
class DownloadService : Service() {
    private var scope: CoroutineScope? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        val first = build("Starting download…", null)
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIF_ID, first, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else startForeground(NOTIF_ID, first)

        scope?.cancel()
        val s = CoroutineScope(Dispatchers.Main + SupervisorJob())
        scope = s
        s.launch {
            Downloads.jobs.collect { jobs ->
                val active = jobs.filter { !it.state.isTerminal }
                if (active.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    val j = active.first()
                    val st = j.state
                    val pct = (st as? DownloadState.Downloading)?.fraction?.let { (it * 100).toInt() }
                    val text = when (st) {
                        is DownloadState.Merging -> "Merging video and audio"
                        is DownloadState.Verifying -> "Checking the file"
                        is DownloadState.Downloading -> if (pct != null) "$pct%" else "Downloading"
                        else -> "Checking link"
                    }
                    val title = if (active.size > 1) "${active.size} downloads" else j.title
                    getSystemService(NotificationManager::class.java).notify(NOTIF_ID, build("$title · $text", pct))
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() { scope?.cancel(); super.onDestroy() }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun build(text: String, pct: Int?): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Video Downloader")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, pct ?: 0, pct == null)
            .build()
    }

    companion object {
        private const val CHANNEL = "downloads"
        private const val NOTIF_ID = 11
        fun start(c: Context) {
            ContextCompat.startForegroundService(c, Intent(c, DownloadService::class.java))
        }
    }
}
