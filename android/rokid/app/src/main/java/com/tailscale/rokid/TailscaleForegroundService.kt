package com.tailscale.rokid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class TailscaleForegroundService : Service() {
    private lateinit var controller: TailscaleDaemonController

    override fun onCreate() {
        super.onCreate()
        controller = TailscaleDaemonController(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        currentSnapshot = when (action) {
            ACTION_START -> controller.start()
            ACTION_SLEEP -> controller.sleep()
            ACTION_RESUME -> controller.resume()
            ACTION_STATUS -> controller.snapshot()
            else -> controller.snapshot(summary = "ignored action=$action")
        }
        startForeground(42, buildNotification(currentSnapshot))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) {
            return
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun buildNotification(snapshot: DaemonSnapshot) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(snapshot.toUiText())
        .setOngoing(true)
        .build()

    companion object {
        const val ACTION_START = "com.tailscale.rokid.action.START"
        const val ACTION_SLEEP = "com.tailscale.rokid.action.SLEEP"
        const val ACTION_RESUME = "com.tailscale.rokid.action.RESUME"
        const val ACTION_STATUS = "com.tailscale.rokid.action.STATUS"
        private const val CHANNEL_ID = "tailscale_rokid_status"
        private var currentSnapshot = DaemonSnapshot(false, false, 0, 0, "not started")

        fun intent(context: Context, action: String): Intent {
            return Intent(context, TailscaleForegroundService::class.java).setAction(action)
        }

        fun snapshot(): DaemonSnapshot = currentSnapshot

        fun handleVoiceAction(context: Context, action: VoiceAction) {
            val serviceAction = when (action) {
                VoiceAction.Start -> ACTION_START
                VoiceAction.Sleep -> ACTION_SLEEP
                VoiceAction.Resume -> ACTION_RESUME
                VoiceAction.Status -> ACTION_STATUS
                VoiceAction.Nodes -> ACTION_STATUS
                is VoiceAction.Unknown -> ACTION_STATUS
            }
            ContextCompat.startForegroundService(context, intent(context, serviceAction))
        }
    }
}
