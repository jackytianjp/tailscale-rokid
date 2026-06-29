package com.tailscale.rokid

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class SpatialHudController(private val context: Context) {
    private var hudView: TextView? = null

    fun show(snapshot: DaemonSnapshot) {
        if (!Settings.canDrawOverlays(context) || hudView != null) {
            return
        }
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 16
        params.y = 32

        hudView = TextView(context).apply {
            text = "Tailscale HUD\n${snapshot.toUiText()}"
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xCC101418.toInt())
            setTextColor(0xFFF2F5F7.toInt())
        }
        windowManager.addView(hudView, params)
    }
}
