package com.tailscale.rokid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class PowerStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> TailscaleForegroundService.ACTION_SLEEP
            Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> TailscaleForegroundService.ACTION_RESUME
            else -> TailscaleForegroundService.ACTION_STATUS
        }
        ContextCompat.startForegroundService(context, TailscaleForegroundService.intent(context, action))
    }
}
